package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringUpstreamRouteRuleServiceComponent, UpstreamRouteRuleServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

object GraduationBenchmarkBreakdownCommand {

  type Result = GraduationBenchmarkBreakdown
  type Command = Appliable[Result] with GraduationBenchmarkBreakdownCommandState

  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
    new GraduationBenchmarkBreakdownCommandInternal(studentCourseDetails, academicYear)
      with ComposableCommand[GraduationBenchmarkBreakdown]
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringUpstreamRouteRuleServiceComponent
      with GraduationBenchmarkBreakdownPermissions
      with GraduationBenchmarkBreakdownCommandState
      with StudentModuleRegistrationAndComponents
      with ReadOnly with Unaudited
}

case class GraduationBenchmarkBreakdown (
  modules: Map[ModuleRegistration, Seq[ComponentAndMarks]],
  graduationBenchmark: BigDecimal,
  totalCats: BigDecimal,
  percentageOfAssessmentTaken: BigDecimal,
)

class GraduationBenchmarkBreakdownCommandInternal(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
  extends CommandInternal[GraduationBenchmarkBreakdown] with TaskBenchmarking {
  self: GraduationBenchmarkBreakdownCommandState
    with StudentModuleRegistrationAndComponents
    with ModuleRegistrationServiceComponent
    with CourseAndRouteServiceComponent
    with UpstreamRouteRuleServiceComponent
    with AssessmentMembershipServiceComponent =>

  override def applyInternal(): GraduationBenchmarkBreakdown = {
    val modules = studentCourseYearDetails.moduleRegistrations.map { mr => mr -> moduleRegistrationService.benchmarkComponentsAndMarks(mr) }.toMap
    GraduationBenchmarkBreakdown(
      modules.filter{ case (_, components) => components.nonEmpty},
      moduleRegistrationService.graduationBenchmark(studentCourseYearDetails.moduleRegistrations),
      studentCourseYearDetails.totalCats,
      moduleRegistrationService.percentageOfAssessmentTaken(studentCourseYearDetails.moduleRegistrations),
    )
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
