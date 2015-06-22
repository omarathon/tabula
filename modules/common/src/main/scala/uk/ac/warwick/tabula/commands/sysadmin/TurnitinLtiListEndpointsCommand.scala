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

object TurnitinLtiListEndpointsCommand {
	def apply(user: CurrentUser) =
		new TurnitinLtiListEndpointsCommandInternal(user)
			with TurnitinLtiListEndpointsCommandPermissions
			with ComposableCommand[Boolean]
			with ReadOnly with Unaudited
			with TurnitinLtiListEndpointsCommandState
			with TurnitinLtiListEndpointsValidation
			with AutowiringTurnitinLtiServiceComponent
			with Logging
}

class TurnitinLtiListEndpointsCommandInternal(val user: CurrentUser) extends CommandInternal[Boolean] {

	self: TurnitinLtiListEndpointsCommandState with TurnitinLtiServiceComponent with Logging =>

	override def applyInternal() = transactional() {

//		val department = assignment.module.adminDepartment
//		val classId = TurnitinLti.classIdFor(assignment, api.classPrefix)
//		val assignmentId = TurnitinLti.assignmentIdFor(assignment)
//		val className = TurnitinLti.classNameFor(assignment)
//		val assignmentName = TurnitinLti.assignmentNameFor(assignment)
//
//		debug(s"Submitting assignment in ${classId.value}, ${assignmentId.value}")
//
		val userEmail = if (user.email == null || user.email.isEmpty) user.firstName + user.lastName + "@TurnitinLti.warwick.ac.uk" else user.email

		turnitinLtiService.listEndpoints(turnitinAssignmentId, user)
		false
	}

}

trait TurnitinLtiListEndpointsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ManageEmergencyMessage)
	}
}

trait TurnitinLtiListEndpointsValidation extends SelfValidating {
	self: TurnitinLtiListEndpointsCommandState =>

	override def validate(errors: Errors) {
		if (turnitinAssignmentId.isEmptyOrWhitespace) {
			errors.rejectValue("turnitinAssignmentId", "turnitin.turnitinassignment.empty")
		}
	}
}

trait TurnitinLtiListEndpointsCommandState {
	var turnitinAssignmentId: String = _
}