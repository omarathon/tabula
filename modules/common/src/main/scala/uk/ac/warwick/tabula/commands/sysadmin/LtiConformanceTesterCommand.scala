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

object LtiConformanceTesterCommand {
	def apply(user: CurrentUser) =
		new LtiConformanceTesterCommandInternal(user)
			with LtiConformanceTesterCommandPermissions
			with ComposableCommand[TurnitinLtiResponse]
			with ReadOnly with Unaudited
			with LtiConformanceTesterCommandState
			with LtiConformanceTesterValidation
			with AutowiringTurnitinLtiServiceComponent
			with Logging
}

class LtiConformanceTesterCommandInternal(val user: CurrentUser) extends CommandInternal[TurnitinLtiResponse] {

	self: LtiConformanceTesterCommandState with TurnitinLtiServiceComponent with Logging =>

	override def applyInternal() = transactional() {
		turnitinLtiService.ltiConformanceTest(endpoint, secret, user)
	}

}

trait LtiConformanceTesterCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ManageEmergencyMessage)
	}
}

trait LtiConformanceTesterValidation extends SelfValidating {
	self: LtiConformanceTesterCommandState =>

	override def validate(errors: Errors) {
		if (endpoint.isEmptyOrWhitespace) {
			errors.rejectValue("endpoint", "turnitin.turnitinassignment.empty", "Please specify an endpoint")
		}
	}
}

trait LtiConformanceTesterCommandState {
	var endpoint: String = _
	var secret: String = _
}