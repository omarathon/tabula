package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import CheckMitCircsEnabledCommand._
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsReportServiceComponent, MitCircsReportServiceComponent, StudentsUnableToSubmitForDepartment}

object CheckMitCircsEnabledCommand {

  type Result = Seq[StudentsUnableToSubmitForDepartment]
  type Command = Appliable[Result]
  val RequiredPermission: Permission = Permissions.ReviewMitCircsDepartments

  def apply() = new CheckMitCircsEnabledCommandInternal()
    with ComposableCommand[Result]
    with CheckMitCircsEnabledPermissions
    with AutowiringMitCircsReportServiceComponent
    with Unaudited with ReadOnly
}

class CheckMitCircsEnabledCommandInternal() extends CommandInternal[Result] {

  self : MitCircsReportServiceComponent =>

  def applyInternal(): Result = transactional() {
    mitCircsReportService.studentsUnableToSubmit
  }
}

trait CheckMitCircsEnabledPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {


  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(RequiredPermission, PermissionsTarget.Global)
  }
}


