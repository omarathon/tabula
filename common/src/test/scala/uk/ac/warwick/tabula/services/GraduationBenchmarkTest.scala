package uk.ac.warwick.tabula.services

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.Fixtures.assessmentGroup
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntityYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{ModuleRegistrationDao, ModuleRegistrationDaoComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

import scala.collection.immutable.SortedMap
import scala.jdk.CollectionConverters._

/**
 * This tests both ProgressionService and ModuleRegistrationService working together to
 * calculate the graduation benchmark.
 */
class GraduationBenchmarkTest extends TestBase with Mockito {

  private trait Fixture {
    val moduleRegistrationService: AbstractModuleRegistrationService with ModuleRegistrationDaoComponent = new AbstractModuleRegistrationService with ModuleRegistrationDaoComponent {
      val moduleRegistrationDao: ModuleRegistrationDao = smartMock[ModuleRegistrationDao]
    }

    val progressionService: AbstractProgressionService with CourseAndRouteServiceComponent = new AbstractProgressionService with ModuleRegistrationServiceComponent with CourseAndRouteServiceComponent {
      override val moduleRegistrationService: ModuleRegistrationService = Fixture.this.moduleRegistrationService
      override val courseAndRouteService: CourseAndRouteService = smartMock[CourseAndRouteService]
    }

    val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]

    // No VAW for now
    assessmentMembershipService.getVariableAssessmentWeightingRules(anyString, anyString) returns Seq.empty
  }

  @Test def wbsExample(): Unit = new Fixture {
    val academicYear: AcademicYear = AcademicYear(2019)
    val course: Course = Fixtures.course("UCSA-WBS1")

    // Student 2 off WBS/Phil Young's spreadsheet
    val student: StudentMember = Fixtures.student("2")
    student.mostSignificantCourse.course = course

    // Student is finalist on a 3 year course
    student.mostSignificantCourse.sprStartAcademicYear = academicYear - 2
    val scyd3: StudentCourseYearDetails = student.mostSignificantCourse.latestStudentCourseYearDetails
    scyd3.yearOfStudy = 3
    scyd3.academicYear = academicYear
    student.mostSignificantCourse.courseYearLength = 3
    scyd3.modeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")

    val scyd2: StudentCourseYearDetails = Fixtures.studentCourseYearDetails()
    scyd2.studentCourseDetails = student.mostSignificantCourse
    scyd2.yearOfStudy = 2
    scyd2.academicYear = academicYear - 1
    scyd2.modeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")

    student.mostSignificantCourse.addStudentCourseYearDetails(scyd2)

    val scyd1: StudentCourseYearDetails = Fixtures.studentCourseYearDetails()
    scyd1.studentCourseDetails = student.mostSignificantCourse
    scyd1.yearOfStudy = 1
    scyd1.academicYear = academicYear - 2
    scyd1.modeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")

    student.mostSignificantCourse.addStudentCourseYearDetails(scyd1)

    def entityYear: ExamGridEntityYear = scyd3.toExamGridEntityYear

    val normalLoad = BigDecimal(120)
    val routeRules: Map[Int, Seq[UpstreamRouteRule]] = Map()

    // 0/50/50 year weightings
    val yearWeighting1: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(0), student.mostSignificantCourse.sprStartAcademicYear, 1)
    val yearWeighting2: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(50), student.mostSignificantCourse.sprStartAcademicYear, 2)
    val yearWeighting3: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(50), student.mostSignificantCourse.sprStartAcademicYear, 3)

    val yearWeightings: Seq[CourseYearWeighting] = Seq(yearWeighting1, yearWeighting2, yearWeighting3)

    val modules = Map(
      "ib232" -> Fixtures.module("ib232"),
      "ib237" -> Fixtures.module("ib237"),
      "ib337" -> Fixtures.module("ib337"),
      "ib357" -> Fixtures.module("ib357"),
      "ib381" -> Fixtures.module("ib381"),
      "ib3f2" -> Fixtures.module("ib3f2"),
      "ib3f6" -> Fixtures.module("ib3f6"),
      "ib3g4" -> Fixtures.module("ib3g4"),
      "ib3k7" -> Fixtures.module("ib3k7"),
    )

    // TODO We give all of these a mark in order to calculate a final year mark but they _shouldn't_ be used
    val moduleRegistrations = Seq(
      Fixtures.moduleRegistration(student.mostSignificantCourse, modules("ib232"), BigDecimal(12).underlying, academicYear, agreedMark = Some(random.nextInt(101))),
      Fixtures.moduleRegistration(student.mostSignificantCourse, modules("ib237"), BigDecimal(12).underlying, academicYear, agreedMark = Some(random.nextInt(101))),
      Fixtures.moduleRegistration(student.mostSignificantCourse, modules("ib337"), BigDecimal(12).underlying, academicYear, agreedMark = Some(random.nextInt(101))),
      Fixtures.moduleRegistration(student.mostSignificantCourse, modules("ib357"), BigDecimal(12).underlying, academicYear, agreedMark = Some(random.nextInt(101))),
      Fixtures.moduleRegistration(student.mostSignificantCourse, modules("ib381"), BigDecimal(24).underlying, academicYear, agreedMark = Some(random.nextInt(101))),
      Fixtures.moduleRegistration(student.mostSignificantCourse, modules("ib3f2"), BigDecimal(12).underlying, academicYear, agreedMark = Some(random.nextInt(101))),
      Fixtures.moduleRegistration(student.mostSignificantCourse, modules("ib3f6"), BigDecimal(12).underlying, academicYear, agreedMark = Some(random.nextInt(101))),
      Fixtures.moduleRegistration(student.mostSignificantCourse, modules("ib3g4"), BigDecimal(12).underlying, academicYear, agreedMark = Some(random.nextInt(101))),
      Fixtures.moduleRegistration(student.mostSignificantCourse, modules("ib3k7"), BigDecimal(12).underlying, academicYear, agreedMark = Some(random.nextInt(101))),
    )

    moduleRegistrations.foreach(_._allStudentCourseDetails = JHashSet(student.mostSignificantCourse))

    val componentsWithDeadlineAndMark: SortedMap[String, Seq[(AssessmentComponent, Option[LocalDate], Option[Int])]] = SortedMap(
      "ib232" -> Seq(
        // Original deadline 2020-03-16 but WBS have special dispensation to include this in the GB
        (Fixtures.assessmentComponent(modules("ib232"), 1, AssessmentType.GroupPresentationMarkedCollectively, 10), Some(LocalDate.parse("2020-03-12")), Some(74)),
        (Fixtures.assessmentComponent(modules("ib232"), 2, AssessmentType.Assignment, 90), Some(LocalDate.parse("2020-05-27")), None),
      ),

      "ib237" -> Seq(
        (Fixtures.assessmentComponent(modules("ib237"), 1, AssessmentType.InClassTest, 20), Some(LocalDate.parse("2019-12-05")), Some(87)),
        (Fixtures.assessmentComponent(modules("ib237"), 2, AssessmentType.SummerExam, 80), Some(LocalDate.parse("2020-06-06")), None),
      ),

      "ib337" -> Seq(
        (Fixtures.assessmentComponent(modules("ib337"), 1, AssessmentType.Assignment), Some(LocalDate.parse("2020-06-01")), Some(61)),
      ),

      "ib357" -> Seq(
        (Fixtures.assessmentComponent(modules("ib357"), 1, AssessmentType.InClassTest, 20), Some(LocalDate.parse("2019-12-03")), Some(30)),
        (Fixtures.assessmentComponent(modules("ib357"), 2, AssessmentType.SummerExam, 80), Some(LocalDate.parse("2020-05-15")), None),
      ),

      "ib381" ->  Seq(
        // Original deadline 2020-03-16 but WBS have special dispensation to include this in the GB
        (Fixtures.assessmentComponent(modules("ib381"), 1, AssessmentType.IndividualPresentation, 30), Some(LocalDate.parse("2020-03-12")), Some(58)),
        // Original deadline 2020-03-17 but WBS have special dispensation to include this in the GB
        (Fixtures.assessmentComponent(modules("ib381"), 2, AssessmentType.ContributionInLearningActivities, 20), Some(LocalDate.parse("2020-03-12")), Some(75)),
        (Fixtures.assessmentComponent(modules("ib381"), 3, AssessmentType.Assignment, 50), Some(LocalDate.parse("2020-05-28")), None),
      ),

      "ib3f2" -> Seq(
        (Fixtures.assessmentComponent(modules("ib3f2"), 1, AssessmentType.InClassTest, 20), Some(LocalDate.parse("2020-02-13")), Some(90)),
        (Fixtures.assessmentComponent(modules("ib3f2"), 2, AssessmentType.Assignment, 80), Some(LocalDate.parse("2020-05-28")), None),
      ),

      "ib3f6" -> Seq(
        (Fixtures.assessmentComponent(modules("ib3f6"), 1, AssessmentType.GroupPresentationMarkedCollectively, 30), Some(LocalDate.parse("2019-12-09")), Some(58)),
        (Fixtures.assessmentComponent(modules("ib3f6"), 2, AssessmentType.Assignment, 70), Some(LocalDate.parse("2020-01-23")), Some(53)),
      ),

      "ib3g4" -> Seq(
        (Fixtures.assessmentComponent(modules("ib3g4"), 1, AssessmentType.GroupPresentationMarkedCollectively, 15), Some(LocalDate.parse("2019-12-02")), Some(65)),
        (Fixtures.assessmentComponent(modules("ib3g4"), 2, AssessmentType.GroupPresentationMarkedIndividually, 10), Some(LocalDate.parse("2019-12-02")), Some(65)),
        (Fixtures.assessmentComponent(modules("ib3g4"), 3, AssessmentType.Assignment, 75), Some(LocalDate.parse("2020-06-01")), None),
      ),

      "ib3k7" -> Seq(
        (Fixtures.assessmentComponent(modules("ib3k7"), 1, AssessmentType.Assignment), Some(LocalDate.parse("2020-05-07")), Some(64)),
      ),
    )
    componentsWithDeadlineAndMark.values.foreach(_.foreach(_._1.membershipService = assessmentMembershipService))

    assessmentMembershipService.getAssessmentComponents(anyString, isEq(false)) answers { args: Array[AnyRef] =>
      componentsWithDeadlineAndMark(Module.stripCats(args(0).asInstanceOf[String]).get.toLowerCase).map(_._1)
    }

    val upstreamAssessmentGroups: Map[String, Seq[UpstreamAssessmentGroup]] = componentsWithDeadlineAndMark.zipWithIndex.map { case ((mc, components), i) =>
      mc -> components.map { case (ac, deadline, mark) =>
        val group = assessmentGroup(ac, academicYear)
        group.deadline = deadline
        val groupMember = new UpstreamAssessmentGroupMember(group, student.universityId)
        groupMember.actualMark = mark
        group.members.clear()
        group.members.addAll(Seq(groupMember).asJava)
        group
      }
    }.toMap

    moduleRegistrations.foreach { mr =>
      student.mostSignificantCourse._moduleRegistrations.add(mr)
      mr.membershipService = assessmentMembershipService
      assessmentMembershipService.getUpstreamAssessmentGroups(mr, eagerLoad = true) returns { upstreamAssessmentGroups(mr.module.code) }
    }

    // Year 1 year mark
    scyd1.agreedMark = BigDecimal(59.0).underlying

    // Year 2 year mark
    scyd2.agreedMark = BigDecimal(45.0).underlying

    progressionService.graduationBenchmark(entityYear.studentCourseYearDetails, entityYear.yearOfStudy, normalLoad, routeRules, calculateYearMarks = false, groupByLevel = false, yearWeightings) should be(
      Right(49.0)
    )
  }

}
