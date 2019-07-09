package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.MitCircsHomeCommand._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesPanel, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.roles.TeachingQualityUser
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsPanelServiceComponent, MitCircsPanelServiceComponent}
import uk.ac.warwick.tabula.services.permissions.{AutowiringRoleServiceComponent, RoleServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

case class MitCircsHomeInfo(
  mcoDepartments: Set[Department],
  panels: Set[MitigatingCircumstancesPanel],
  submissions: Set[MitigatingCircumstancesSubmission]
)

object MitCircsHomeCommand {

  type Result = MitCircsHomeInfo
  type Command = Appliable[Result] with MitCircsHomeState

  def apply(currentUser: CurrentUser) = new MitCircsHomeCommandInternal(currentUser)
    with ComposableCommand[Result]
    with PubliclyVisiblePermissions
    with MitCircsHomeDescription with ReadOnly
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringMitCircsPanelServiceComponent
    with AutowiringRoleServiceComponent
}

class MitCircsHomeCommandInternal(val currentUser: CurrentUser) extends CommandInternal[Result] with MitCircsHomeState {

  self: ModuleAndDepartmentServiceComponent with MitCircsPanelServiceComponent with RoleServiceComponent =>

  def applyInternal(): Result = {
    val departments = if (roleService.hasRole(currentUser, TeachingQualityUser())){
      moduleAndDepartmentService.allDepartments.filter(_.enableMitCircs).toSet
    } else {
      moduleAndDepartmentService.departmentsWithPermission(currentUser, Permissions.MitigatingCircumstancesSubmission.Manage)
    }
    val panels = mitCircsPanelService.getPanels(MemberOrUser(currentUser.apparentUser))
    // TODO - model permissions granted to individual mitcircs submissions and show them here
    val submissions = Set[MitigatingCircumstancesSubmission]()
    MitCircsHomeInfo(departments, panels, submissions)
  }
}

trait MitCircsHomeDescription extends Describable[Result] {
  override lazy val eventName: String = "MitCircsHome"

  def describe(d: Description) { /* Do nothing - just audit that this page was visited by the user */ }
}

trait MitCircsHomeState {
  val currentUser: CurrentUser
}
