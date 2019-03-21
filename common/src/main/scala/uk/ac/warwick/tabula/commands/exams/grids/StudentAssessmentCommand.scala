package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object StudentAssessmentCommand {
  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
    new StudentAssessmentCommandInternal(studentCourseDetails, academicYear)
      with AutowiringStudentCourseYearDetailsDaoComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with ComposableCommand[StudentMarksBreakdown]
      with StudentAssessmentPermissions
      with StudentAssessmentCommandState
      with ReadOnly with Unaudited
}

object StudentAssessmentProfileCommand {

  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
    new StudentAssessmentCommandInternal(studentCourseDetails, academicYear)
      with ComposableCommand[StudentMarksBreakdown]
      with AutowiringStudentCourseYearDetailsDaoComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with StudentAssessmentProfilePermissions
      with StudentAssessmentCommandState
      with ReadOnly with Unaudited
}

case class StudentMarksBreakdown(
  yearMark: Option[BigDecimal],
  yearWeighting: Option[CourseYearWeighting],
  modules: Seq[ModuleRegistrationAndComponents]
)

case class ModuleRegistrationAndComponents(
  moduleRegistration: ModuleRegistration,
  components: Seq[Component]
)

case class Component(upstreamGroup: UpstreamGroup, member: UpstreamAssessmentGroupMember)

class StudentAssessmentCommandInternal(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
  extends CommandInternal[StudentMarksBreakdown] with TaskBenchmarking {

  self: StudentAssessmentCommandState with StudentCourseYearDetailsDaoComponent with AssessmentMembershipServiceComponent
    with ModuleRegistrationServiceComponent with CourseAndRouteServiceComponent =>

  override def applyInternal(): StudentMarksBreakdown = {
    val modules = studentCourseYearDetails.moduleRegistrations.map { mr =>
      val components = for {
        uagm <- mr.upstreamAssessmentGroupMembers
        aComponent <- assessmentMembershipService.getAssessmentComponent(uagm.upstreamAssessmentGroup)
      } yield Component(new UpstreamGroup(aComponent, uagm.upstreamAssessmentGroup, mr.currentUpstreamAssessmentGroupMembers), uagm)
      ModuleRegistrationAndComponents(mr, components)
    }

    val yearMark = moduleRegistrationService.agreedWeightedMeanYearMark(studentCourseYearDetails.moduleRegistrations, Map(), allowEmpty = false).toOption
    val yearWeighting = courseAndRouteService.getCourseYearWeighting(
      studentCourseYearDetails.studentCourseDetails.course.code,
      studentCourseYearDetails.academicYear,
      studentCourseYearDetails.yearOfStudy
    )

    StudentMarksBreakdown(yearMark, yearWeighting, modules)
  }
}


trait StudentAssessmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: StudentAssessmentCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheckAny(
      Seq(CheckablePermission(Permissions.Department.ExamGrids, studentCourseYearDetails.enrolmentDepartment),
        CheckablePermission(Permissions.Department.ExamGrids, studentCourseYearDetails.studentCourseDetails.currentRoute))
    )
  }

}

trait StudentAssessmentProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentAssessmentCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Profiles.Read.ModuleRegistration.Core, studentCourseDetails)
  }
}


trait StudentAssessmentCommandState {
  def academicYear: AcademicYear

  def studentCourseDetails: StudentCourseDetails

  lazy val studentCourseYearDetails: StudentCourseYearDetails = studentCourseDetails.freshStudentCourseYearDetailsForYear(academicYear) // fresh scyd for this year
    .orElse(studentCourseDetails.freshOrStaleStudentCourseYearDetailsForYear(academicYear)) // or stale scyd for this year
    .getOrElse(throw new ItemNotFoundException())
}
