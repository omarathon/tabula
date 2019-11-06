package uk.ac.warwick.tabula.commands.sysadmin

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.turnitinlti._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object TurnitinLtiSubmissionDetailsCommand {
  def apply() =
    new TurnitinLtiSubmissionDetailsCommandInternal
      with TurnitinLtiSubmissionDetailsCommandPermissions
      with ComposableCommand[TurnitinLtiResponse]
      with ReadOnly with Unaudited
      with TurnitinLtiSubmissionDetailsCommandState
      with TurnitinLtiSubmissionDetailsValidation
      with AutowiringTurnitinLtiServiceComponent
}

class TurnitinLtiSubmissionDetailsCommandInternal extends CommandInternal[TurnitinLtiResponse] {

  self: TurnitinLtiSubmissionDetailsCommandState with TurnitinLtiServiceComponent =>

  override def applyInternal(): TurnitinLtiResponse = transactional() {
    turnitinLtiService.getSubmissionDetails(turnitinSubmissionId)
  }

}

trait TurnitinLtiSubmissionDetailsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.ImportSystemData)
  }
}

trait TurnitinLtiSubmissionDetailsValidation extends SelfValidating {
  self: TurnitinLtiSubmissionDetailsCommandState =>

  override def validate(errors: Errors): Unit = {
    if (turnitinSubmissionId.isEmptyOrWhitespace) {
      errors.rejectValue("turnitinSubmissionId", "turnitin.submission.empty")
    }
  }
}

trait TurnitinLtiSubmissionDetailsCommandState {
  var turnitinSubmissionId: String = _
}
