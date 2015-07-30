package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions

import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.turnitinlti._

import uk.ac.warwick.tabula.helpers.StringUtils._

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.helpers.Logging
import org.springframework.validation.Errors

object TurnitinLtiViewReportCommand {
	def apply(user: CurrentUser) =
		new TurnitinLtiViewReportCommandInternal(user)
				with TurnitinLtiViewReportCommandPermissions
				with ComposableCommand[TurnitinLtiResponse]
			with ReadOnly with Unaudited
			with TurnitinLtiViewReportCommandState
			with TurnitinLtiViewReportValidation
			with AutowiringTurnitinLtiServiceComponent
			with Logging
}

class TurnitinLtiViewReportCommandInternal(val user: CurrentUser) extends CommandInternal[TurnitinLtiResponse]{

	self: TurnitinLtiViewReportCommandState with TurnitinLtiServiceComponent with Logging =>

	override def applyInternal() = transactional() {
		turnitinLtiService.getOriginalityReportUrl(turnitinSubmissionId, user)
	}

}

trait TurnitinLtiViewReportCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ManageEmergencyMessage)
	}
}

trait TurnitinLtiViewReportValidation extends SelfValidating {
	self: TurnitinLtiViewReportCommandState =>

	override def validate(errors: Errors) {
		if (turnitinSubmissionId.isEmptyOrWhitespace) {
			errors.rejectValue("turnitinSubmissionId", "turnitin.submission.empty")
		}
	}
}

trait TurnitinLtiViewReportCommandState {
//	var assignment: Assignment = _
	var turnitinSubmissionId: String = _
}