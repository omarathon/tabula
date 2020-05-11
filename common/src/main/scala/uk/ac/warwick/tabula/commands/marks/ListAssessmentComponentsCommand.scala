package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

object ListAssessmentComponentsCommand {
  case class AssessmentComponentInfo(
    assessmentComponent: AssessmentComponent,
    upstreamAssessmentGroupInfo: UpstreamAssessmentGroupInfo,
  ) {
    lazy val currentMembersWithMarksUploaded: Seq[UpstreamAssessmentGroupMember] =
      upstreamAssessmentGroupInfo.currentMembers.filter(_.actualMark.nonEmpty)
  }
  type Result = Seq[AssessmentComponentInfo]
  type Command = Appliable[Result]

  val AdminPermission: Permission = Permissions.Feedback.Manage

  def apply(department: Department, academicYear: AcademicYear, currentUser: CurrentUser): Command =
    new ListAssessmentComponentsCommandInternal(department, academicYear, currentUser)
      with ComposableCommand[Result]
      with ListAssessmentComponentsModulesWithPermission
      with ListAssessmentComponentsPermissions
      with Unaudited with ReadOnly
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
}

abstract class ListAssessmentComponentsCommandInternal(val department: Department, val academicYear: AcademicYear, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with ListAssessmentComponentsState {
  self: AssessmentMembershipServiceComponent
    with ListAssessmentComponentsModulesWithPermission
    with SecurityServiceComponent
    with ModuleAndDepartmentServiceComponent =>

  override def applyInternal(): Result = {
    val assessmentComponents: Seq[AssessmentComponent] =
      assessmentMembershipService.getAssessmentComponents(department, includeSubDepartments = false)
        .filter { ac =>
          ac.assessmentGroup != "AO" &&
          ac.sequence != AssessmentComponent.NoneAssessmentGroup &&
          (canAdminDepartment || modulesWithPermission.contains(ac.module))
        }

    val assessmentComponentsByKey: Map[AssessmentComponentKey, AssessmentComponent] =
      assessmentComponents.map { ac =>
        AssessmentComponentKey(ac) -> ac
      }.toMap

    assessmentMembershipService.getUpstreamAssessmentGroupInfoForComponents(assessmentComponents, academicYear)
      .filter(_.allMembers.nonEmpty)
      .map { upstreamAssessmentGroupInfo =>
        AssessmentComponentInfo(
          assessmentComponentsByKey(AssessmentComponentKey(upstreamAssessmentGroupInfo.upstreamAssessmentGroup)),
          upstreamAssessmentGroupInfo,
        )
      }
      .sortBy { info =>
        // module_code, assessment_group, sequence, mav_occurrence
        (info.assessmentComponent.moduleCode, info.assessmentComponent.assessmentGroup, info.assessmentComponent.sequence, info.upstreamAssessmentGroupInfo.upstreamAssessmentGroup.occurrence)
      }
  }

}

trait ListAssessmentComponentsState {
  def department: Department
  def academicYear: AcademicYear
  def currentUser: CurrentUser
}

trait ListAssessmentComponentsModulesWithPermission {
  self: ListAssessmentComponentsState
    with SecurityServiceComponent
    with ModuleAndDepartmentServiceComponent =>

  lazy val canAdminDepartment: Boolean = securityService.can(currentUser, AdminPermission, department)
  lazy val modulesWithPermission: Set[Module] = moduleAndDepartmentService.modulesWithPermission(currentUser, AdminPermission, department)
}

trait ListAssessmentComponentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ListAssessmentComponentsModulesWithPermission
    with ListAssessmentComponentsState
    with SecurityServiceComponent
    with ModuleAndDepartmentServiceComponent =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    if (canAdminDepartment || modulesWithPermission.isEmpty) p.PermissionCheck(AdminPermission, mandatory(department))
    else p.PermissionCheckAll(AdminPermission, modulesWithPermission)
}
