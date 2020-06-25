package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, AutowiringUpstreamRouteRuleServiceComponent, NormalCATSLoadServiceComponent, NormalLoadLookup, UpstreamRouteRuleServiceComponent}
import uk.ac.warwick.tabula.services.marks.{AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

import scala.math.BigDecimal.RoundingMode

object GraduationBenchmarkBreakdownCommand {

  type Result = Either[UGGraduationBenchmarkBreakdown, PGGraduationBenchmarkBreakdown]
  type Command = Appliable[Result] with GraduationBenchmarkBreakdownCommandState

  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear): Command =
    new GraduationBenchmarkBreakdownCommandInternal(studentCourseDetails, academicYear)
      with ComposableCommand[Either[UGGraduationBenchmarkBreakdown, PGGraduationBenchmarkBreakdown]]
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringProgressionServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringUpstreamRouteRuleServiceComponent
      with AutowiringNormalCATSLoadServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with GraduationBenchmarkBreakdownPermissions
      with GraduationBenchmarkBreakdownCommandState
      with GraduationBenchmarkBreakdownCommandRequest
      with StudentModuleRegistrationAndComponents
      with ReadOnly with Unaudited {
      override val includeActualMarks: Boolean = true
    }
}

case class UGGraduationBenchmarkBreakdown (
  modules: Map[ModuleRegistration, Seq[ComponentAndMarks]],
  weightedAssessmentMark: BigDecimal,
  totalCats: BigDecimal,
  percentageOfAssessmentTaken: BigDecimal,
  percentageOfAssessmentTakenDecimal: BigDecimal,
  marksAndWeightingsPerYear: Map[Int, (BigDecimal, BigDecimal)],
  graduationBenchmark: Option[BigDecimal],
  benchmarkErrors: Seq[String]
)

case class PGGraduationBenchmarkBreakdown (
  modules: Seq[ModuleRegistration],
  graduationBenchmark: BigDecimal,
  minCatsToConsider: BigDecimal,
  totalCatsTaken: BigDecimal,
  usedModulesWithCumulativeSums: Seq[CumulativeCatsAndMarks],
  unusedModules: Seq[ModuleRegistration],
  catsConsidered: BigDecimal
)

case class CumulativeCatsAndMarks (
  moduleRegistration: ModuleRegistration,
  marks: BigDecimal,
  cats: BigDecimal,
)


class GraduationBenchmarkBreakdownCommandInternal(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
  extends CommandInternal[Either[UGGraduationBenchmarkBreakdown, PGGraduationBenchmarkBreakdown]] with TaskBenchmarking {

  self: GraduationBenchmarkBreakdownCommandState
    with GraduationBenchmarkBreakdownCommandRequest
    with StudentModuleRegistrationAndComponents
    with ModuleRegistrationServiceComponent
    with ProgressionServiceComponent
    with CourseAndRouteServiceComponent
    with UpstreamRouteRuleServiceComponent
    with AssessmentMembershipServiceComponent =>

  override def applyInternal(): Either[UGGraduationBenchmarkBreakdown, PGGraduationBenchmarkBreakdown] = {
    val moduleRegistrations = studentCourseYearDetails.moduleRegistrations
    val modules = moduleRegistrations.map { mr => mr -> moduleRegistrationService.benchmarkComponentsAndMarks(mr) }.toMap
    if (studentCourseDetails.student.isUG) {

      val weightedAssessmentMark = moduleRegistrationService.benchmarkWeightedAssessmentMark(studentCourseYearDetails.moduleRegistrations)

      val percentageOfAssessmentTaken = moduleRegistrationService.percentageOfAssessmentTaken(studentCourseYearDetails.moduleRegistrations).setScale(1, RoundingMode.HALF_UP)
      val percentageOfAssessmentTakenDecimal = percentageOfAssessmentTaken / 100

      def weighting(year: Int): BigDecimal = weightings.find(_.yearOfStudy == year).map(w => BigDecimal(w.weightingAsPercentage)).getOrElse(BigDecimal(0))

      val yearMarksAndWeightings = progressionService.marksPerYear(studentCourseYearDetails, normalLoad, routeRulesPerYear, calculateYearMarks, groupByLevel, weightings, markForFinalYear = false)
        .map { _.map { case (year, weightedMark) =>
          val mark = if (year == yearOfStudy) weightedAssessmentMark else weightedMark
          year -> (mark, weighting(year))
        } + (yearOfStudy -> (weightedAssessmentMark, weighting(yearOfStudy)))
      }

      val benchmark = progressionService.graduationBenchmark(Option(studentCourseYearDetails), yearOfStudy, normalLoad, routeRulesPerYear, calculateYearMarks, groupByLevel, weightings)

      Left(UGGraduationBenchmarkBreakdown(
        modules = modules.filter{ case (_, components) => components.nonEmpty },
        weightedAssessmentMark,
        totalCats = studentCourseYearDetails.totalCats,
        percentageOfAssessmentTaken,
        percentageOfAssessmentTakenDecimal,
        marksAndWeightingsPerYear = yearMarksAndWeightings.toOption.getOrElse(Map()),
        graduationBenchmark = benchmark.toOption,
        benchmarkErrors = yearMarksAndWeightings.swap.toSeq ++ benchmark.swap.toSeq
      ))
    } else {
      val minCatsToConsider = progressionService.numberCatsToConsiderPG(studentCourseYearDetails)
      val (bestPGModules, catsConsidered) = progressionService.bestPGModules(moduleRegistrations, minCatsToConsider)
      val usedModulesWithCumulativeSums: Seq[CumulativeCatsAndMarks] = bestPGModules
        .foldLeft(Nil: Seq[CumulativeCatsAndMarks]) { case (acc, mr) =>
          val catsScaledMark = BigDecimal(mr.firstDefinedMark.get) * BigDecimal(mr.cats)
          if(acc.isEmpty) Seq(CumulativeCatsAndMarks(mr, catsScaledMark, BigDecimal(mr.cats)))
          else acc :+ CumulativeCatsAndMarks(mr, acc.last.marks + catsScaledMark, acc.last.cats + BigDecimal(mr.cats))
        }

      Right(PGGraduationBenchmarkBreakdown(
        modules = moduleRegistrations.sortBy(mr => (mr.firstDefinedMark, mr.cats)).reverse,
        graduationBenchmark = progressionService.postgraduateBenchmark(studentCourseYearDetails, moduleRegistrations),
        minCatsToConsider,
        totalCatsTaken = moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum,
        usedModulesWithCumulativeSums,
        unusedModules = (moduleRegistrations diff bestPGModules).sortBy(mr => (mr.firstDefinedMark, mr.cats)).reverse,
        catsConsidered,
      ))
    }
  }
}

trait GraduationBenchmarkBreakdownPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: GraduationBenchmarkBreakdownCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheckAny(
      Seq(CheckablePermission(Permissions.Department.ExamGrids, studentCourseYearDetails.enrolmentDepartment),
        CheckablePermission(Permissions.Department.ExamGrids, studentCourseYearDetails.studentCourseDetails.currentRoute))
    )
  }

}

trait GraduationBenchmarkBreakdownCommandState {

  self: GraduationBenchmarkBreakdownCommandRequest with NormalCATSLoadServiceComponent with CourseAndRouteServiceComponent
    with UpstreamRouteRuleServiceComponent =>

  def academicYear: AcademicYear
  def studentCourseDetails: StudentCourseDetails

  lazy val studentCourseYearDetails: StudentCourseYearDetails = studentCourseDetails.freshStudentCourseYearDetailsForYear(academicYear)
    .orElse(studentCourseDetails.freshOrStaleStudentCourseYearDetailsForYear(academicYear))
    .getOrElse(throw new ItemNotFoundException())

  lazy val yearOfStudy: Int = if(groupByLevel) studentCourseYearDetails.level.map(_.toYearOfStudy).getOrElse(1) else studentCourseYearDetails.yearOfStudy

  lazy val normalLoad: BigDecimal = NormalLoadLookup(academicYear, yearOfStudy, normalCATSLoadService).apply(studentCourseYearDetails.route)

  lazy val weightings: Seq[CourseYearWeighting] = courseAndRouteService
    .findAllCourseYearWeightings(Seq(studentCourseDetails.course), studentCourseDetails.sprStartAcademicYear)

  lazy val routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]] = studentCourseDetails.freshOrStaleStudentCourseYearDetails.flatMap(_.level)
    .map(level => level.toYearOfStudy -> UpstreamRouteRuleLookup(academicYear, upstreamRouteRuleService).apply(studentCourseYearDetails.route, Option(level)))
    .toMap
}

trait GraduationBenchmarkBreakdownCommandRequest {
  var groupByLevel: Boolean = false
  var calculateYearMarks: Boolean = false
}
