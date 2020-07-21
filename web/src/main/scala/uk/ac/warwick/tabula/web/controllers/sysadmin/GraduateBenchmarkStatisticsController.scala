package uk.ac.warwick.tabula.web.controllers.sysadmin

import java.io.PrintWriter

import org.hibernate.FetchMode
import org.hibernate.criterion.Projections._
import org.hibernate.criterion.Restrictions._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.columns.ExamGridYearMarksToUse
import uk.ac.warwick.tabula.helpers.RequestLevelCache
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, AutowiringUpstreamRouteRuleServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProgressionServiceComponent, FinalYearGrade, FinalYearMark}

import scala.util.Try

@Controller
@RequestMapping(Array("/sysadmin/graduation-benchmark-stats"))
class GraduateBenchmarkStatisticsController extends BaseSysadminController
  with AutowiringProgressionServiceComponent
  with Daoisms
  with AutowiringNormalCATSLoadServiceComponent
  with AutowiringUpstreamRouteRuleServiceComponent {

  // Get all finalist students (19/20 SCYD with yearOfStudy == SCD.courseYearLength
  // Coursecode like 'U%'
  // Route code not in 'uoes', 'ueso'
  @ModelAttribute("finalists")
  def finalists: Seq[ExamGridEntity] = {
    type UniversityId = String
    type StudentCourseYearDetailsId = String

    val studentsMap: Map[UniversityId, StudentCourseYearDetailsId] =
      session.newCriteria[StudentCourseYearDetails]
        .createAlias("studentCourseDetails", "scd")
        .createAlias("enrolmentDepartment", "enrolmentDepartment")
        .add(isNull("missingFromImportSince"))
        .add(is("academicYear", AcademicYear.starting(2019)))
        .add(is("enrolledOrCompleted", true))
        .add(not(like("scd.statusOnRoute.code", "T%"))) // TWD
        .add(not(like("scd.statusOnRoute.code", "D%"))) // Deceased
        .add(eqProperty("yearOfStudy", "scd.courseYearLength"))
        .add(like("scd.course.code", "U%"))
        .add(not(is("enrolmentDepartment.code", "ib"))) // No WBS
        .add(not(in("route.code", "uoes", "ueso")))
        .add(not(in("scd.course.code", "UHIA-VM11", "UHIA-VM12")))
        .project[Array[Any]](
          projectionList()
            .add(property("scd.student.universityId"), "universityId")
            .add(property("id"), "id")
        )
        .seq
        .map { case Array(universityId: UniversityId, scydId: StudentCourseYearDetailsId) => (universityId, scydId) }
        .toMap

    // Eagerly load the students and relationships for use later
    if (studentsMap.isEmpty) Seq.empty
    else session.newCriteria[StudentMember]
      .add(safeIn("universityId", studentsMap.keys.toSeq))
      .setFetchMode("studentCourseDetails", FetchMode.JOIN)
      .setFetchMode("studentCourseDetails._moduleRegistrations", FetchMode.JOIN)
      .setFetchMode("studentCourseDetails._moduleRegistrations.module", FetchMode.JOIN)
      .setFetchMode("studentCourseDetails._moduleRegistrations._recordedModuleRegistration", FetchMode.JOIN)
      .setFetchMode("studentCourseDetails.currentRoute", FetchMode.JOIN)
      .setFetchMode("studentCourseDetails.studentCourseYearDetails", FetchMode.JOIN)
      .setFetchMode("studentCourseDetails.studentCourseYearDetails.route", FetchMode.JOIN)
      .distinct
      .seq
      .map { student =>
        student.freshOrStaleStudentCourseDetails.flatMap(_.freshOrStaleStudentCourseYearDetails)
          .find(_.id == studentsMap(student.universityId))
          .get
      }
      .map { scyd =>
        scyd.studentCourseDetails.student.toExamGridEntity(scyd, basedOnLevel = false, mitCircs = Seq.empty)
      }
  }

  private def normalLoad(route: Route, yearOfStudy: Int): BigDecimal = RequestLevelCache.cachedBy("NormalCATSLoadService.find", s"${route.code}-$yearOfStudy") {
    normalCATSLoadService.find(route, AcademicYear.starting(2019), yearOfStudy).map(_.normalLoad)
      .getOrElse(Option(route).map(_.degreeType).getOrElse(DegreeType.Undergraduate).normalCATSLoad)
  }

  private def routeRules(route: Route, level: Option[Level]): Seq[UpstreamRouteRule] = RequestLevelCache.cachedBy("UpstreamRouteRulesService.find", s"${route.code}-${level.map(_.code).getOrElse("-")}") {
    level.map { l =>
      upstreamRouteRuleService.list(route, AcademicYear.starting(2019), l)
    }.getOrElse(Seq.empty)
  }

  @ModelAttribute("results")
  def results(@ModelAttribute("finalists") finalists: Seq[ExamGridEntity]): Seq[(ExamGridEntity, Either[String, BigDecimal], Either[String, BigDecimal])] = {
    // ExamGridEntity, Weighted mean, Graduation benchmark
    finalists.map { entity =>
      val entityYear = entity.validYears.maxBy(_._1)._2
      val routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]] = entity.validYears.view.mapValues(ey => routeRules(ey.route, ey.level)).toMap

      val graduationBenchmark: Either[String, BigDecimal] = Try(progressionService.graduationBenchmark(
        entityYear.studentCourseYearDetails,
        entityYear.yearOfStudy,
        normalLoad(entityYear.route, entityYear.yearOfStudy),
        routeRulesPerYear,
        ExamGridYearMarksToUse.UploadedYearMarksIfAvailable,
        groupByLevel = false,
        entity.yearWeightings,
      )).recover { case t: Throwable => Left(t.getMessage) }.get

      val finalOverallMark: Either[String, BigDecimal] = Try(progressionService.suggestedFinalYearGrade(
        entityYear,
        normalLoad(entityYear.route, entityYear.yearOfStudy),
        routeRulesPerYear,
        ExamGridYearMarksToUse.UploadedYearMarksIfAvailable,
        groupByLevel = false,
        applyBenchmark = false,
        entity.yearWeightings
      ) match {
        case unknown: FinalYearGrade.Unknown => Left(unknown.reason)
        case withMark: FinalYearMark => Right(withMark.mark)
        case result => Left(result.description)
      }).recover { case t: Throwable => Left(t.getMessage) }.get

      (entity, graduationBenchmark, finalOverallMark)
    }
  }

  @ModelAttribute("stats")
  def stats(@ModelAttribute("results") results: Seq[(ExamGridEntity, Either[String, BigDecimal], Either[String, BigDecimal])]): Seq[(Department, (Int, Int, Int, Int, Int))] = {
    results.groupBy(_._1.years.last._2.get.studentCourseYearDetails.get.enrolmentDepartment)
      .toSeq
      .sortBy(_._1.code)
      .map { case (department, students) =>
        val hasCalculations =
          students.filter { case (_, gb, ym) => gb.isRight && ym.isRight }
            .map { case (student, gb, ym) => (student, gb.getOrElse(BigDecimal(0)), ym.getOrElse(BigDecimal(0))) }

        val classifications =
          hasCalculations.map { case (student, gb, ym) =>
            def toClassification(mark: BigDecimal) =
              if (mark < 40) 0
              else if (mark < 50) 4
              else if (mark < 60) 5
              else if (mark < 70) 6
              else 7

            (student, toClassification(gb), toClassification(ym))
          }

        // Student count, students with valid marks, students with higher GB than YM, fail-to-pass, students with higher degree classification
        department -> (
          students.size,
          hasCalculations.size,
          hasCalculations.count { case (_, gb, ym) => gb > ym },
          classifications.count { case (_, gb, ym) => ym == 0 && gb > 0 },
          classifications.count { case (_, gb, ym) => gb > ym }
        )
      }
  }

  @RequestMapping
  def view(@ModelAttribute("stats") stats: Seq[(Department, (Int, Int, Int, Int, Int))], writer: PrintWriter): Unit = {
    writer.println(s"Department code\tDepartment name\tFinalists\tWith calculation\tGraduate benchmark used\tFail -> Pass\tHigher classification")
    stats.foreach { case (department, (students, withCalculation, graduationBenchmarkUsed, failToPass, higherClassification)) =>
      writer.println(s"${department.code}\t${department.name}\t$students\t$withCalculation\t$graduationBenchmarkUsed\t$failToPass\t$higherClassification")
    }
  }

}
