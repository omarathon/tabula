package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCsrfServiceComponent, AutowiringSettingsSyncQueueComponent, CsrfEnforceMessage, CsrfServiceComponent, SettingsSyncQueueComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object CsrfEnforceCommand {
  def apply() = new CsrfEnforceCommandInternal
    with ComposableCommand[Unit]
    with AutowiringCsrfServiceComponent
    with AutowiringSettingsSyncQueueComponent
    with Unaudited
    with CsrfEnforceCommandPermissions
}

class CsrfEnforceCommandInternal extends CommandInternal[Unit] with PopulateOnForm {
  self: CsrfServiceComponent with SettingsSyncQueueComponent =>

  var enforce: Boolean = _

  override def populate(): Unit = {
    enforce = csrfService.enforce
  }

  override protected def applyInternal(): Unit = {
    csrfService.enforce = enforce

    settingsSyncQueue.send(new CsrfEnforceMessage(enforce))
  }
}

trait CsrfEnforceCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.GodMode)
  }

}
