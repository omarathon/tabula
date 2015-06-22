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

object TurnitinLtiSubmissionDetailsCommand {
	def apply(user: CurrentUser) =
		new TurnitinLtiSubmissionDetailsCommandInternal(user)
				with TurnitinLtiSubmissionDetailsCommandPermissions
				with ComposableCommand[Boolean]
			with ReadOnly with Unaudited
			with TurnitinLtiSubmissionDetailsCommandState
			with TurnitinLtiSubmissionDetailsValidation
			with AutowiringTurnitinLtiServiceComponent
			with Logging
}

class TurnitinLtiSubmissionDetailsCommandInternal(val user: CurrentUser) extends CommandInternal[Boolean]{

	self: TurnitinLtiSubmissionDetailsCommandState with TurnitinLtiServiceComponent with Logging =>

	override def applyInternal() = transactional() {

//		debug(s"Submitting assignment in ${classId.value}, ${assignmentId.value}")
//
		val userEmail = if (user.email == null || user.email.isEmpty) user.firstName + user.lastName + "@TurnitinLti.warwick.ac.uk" else user.email

		turnitinLtiService.getSubmissionDetails(turnitinSubmissionId, user)
		false
	}

}

trait TurnitinLtiSubmissionDetailsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ManageEmergencyMessage)
	}
}

trait TurnitinLtiSubmissionDetailsValidation extends SelfValidating {
	self: TurnitinLtiSubmissionDetailsCommandState =>

	override def validate(errors: Errors) {
		if (turnitinSubmissionId.isEmptyOrWhitespace) {
			errors.rejectValue("turnitinSubmissionId", "turnitin.submission.empty")
		}
	}
}

trait TurnitinLtiSubmissionDetailsCommandState {
//	var assignment: Assignment = _
	var turnitinSubmissionId: String = _
}