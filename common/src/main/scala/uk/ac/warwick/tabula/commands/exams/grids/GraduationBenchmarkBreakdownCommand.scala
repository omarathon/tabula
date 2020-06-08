package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringUpstreamRouteRuleServiceComponent, UpstreamRouteRuleServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

object GraduationBenchmarkBreakdownCommand {

  type Result = Either[UGGraduationBenchmarkBreakdown, PGGraduationBenchmarkBreakdown]
  type Command = Appliable[Result] with GraduationBenchmarkBreakdownCommandState

  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
    new GraduationBenchmarkBreakdownCommandInternal(studentCourseDetails, academicYear)
      with ComposableCommand[Either[UGGraduationBenchmarkBreakdown, PGGraduationBenchmarkBreakdown]]
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringProgressionServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringUpstreamRouteRuleServiceComponent
      with GraduationBenchmarkBreakdownPermissions
      with GraduationBenchmarkBreakdownCommandState
      with StudentModuleRegistrationAndComponents
      with ReadOnly with Unaudited
}

case class UGGraduationBenchmarkBreakdown (
  modules: Map[ModuleRegistration, Seq[ComponentAndMarks]],
  graduationBenchmark: BigDecimal,
  totalCats: BigDecimal,
  percentageOfAssessmentTaken: BigDecimal,
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
      Left(UGGraduationBenchmarkBreakdown(
        modules.filter{ case (_, components) => components.nonEmpty },
        moduleRegistrationService.benchmarkWeightedAssessmentMark(studentCourseYearDetails.moduleRegistrations),
        studentCourseYearDetails.totalCats,
        moduleRegistrationService.percentageOfAssessmentTaken(studentCourseYearDetails.moduleRegistrations),
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
  def academicYear: AcademicYear
  def studentCourseDetails: StudentCourseDetails

  lazy val studentCourseYearDetails: StudentCourseYearDetails = studentCourseDetails.freshStudentCourseYearDetailsForYear(academicYear)
    .orElse(studentCourseDetails.freshOrStaleStudentCourseYearDetailsForYear(academicYear))
    .getOrElse(throw new ItemNotFoundException())
}
