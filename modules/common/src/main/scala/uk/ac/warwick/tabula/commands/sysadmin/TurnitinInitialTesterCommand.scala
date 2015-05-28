package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions

import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.turnitinlti._

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.web.turnitinlti.TurnitinLtiTrait
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.turnitinlti.AssignmentId

object TurnitinInitialTesterCommand {
	def apply(user: CurrentUser) =
		new TurnitinInitialTesterCommandInternal(user)
				with TurnitinInitialTesterCommandPermissions
				with ComposableCommand[Boolean]
			with ReadOnly with Unaudited
			with TurnitinInitialTesterCommandState
			with Logging
}

class TurnitinInitialTesterCommandInternal(val user: CurrentUser) extends CommandInternal[Boolean]
	with TurnitinLtiTrait {

	self: TurnitinInitialTesterCommandState with Logging =>

	var anapi: TurnitinLti = Wire.auto[TurnitinLti]
	var topLevelUrl: String = Wire.property("${toplevel.url}")

	override def applyInternal() = transactional() {

		val department = assignment.module.adminDepartment
		val classId = TurnitinLti.classIdFor(assignment, api.classPrefix)
		val assignmentId = TurnitinLti.assignmentIdFor(assignment)
		val className = TurnitinLti.classNameFor(assignment)
		val assignmentName = TurnitinLti.assignmentNameFor(assignment)

		debug(s"Submitting assignment in ${classId.value}, ${assignmentId.value}")

		val session = new TurnitinLtiSession(anapi, null)
		val userEmail = if (user.email == null || user.email.isEmpty) user.firstName + user.lastName + "@TurnitinLti.warwick.ac.uk" else user.email

		session.userEmail = userEmail
		session.userFirstName = user.firstName
		session.userLastName = user.lastName

		submitAssignment(session, assignmentId, assignmentName, classId, className)

		assignment.isClosed
	}

	// put into a service which can be shared with the scheduled job
	def submitAssignment(session: TurnitinLtiSession, assignmentId: AssignmentId, assignmentName: AssignmentName, classId: ClassId, className: ClassName) {
		val response = session.doRequestAdvanced(None,
			"lis_person_contact_email_primary" -> user.email,
			"lis_person_contact_name_given" -> user.firstName,
			"lis_person_contact_name_family" -> user.lastName,
			"resource_link_id" -> assignmentId.value,
			"resource_link_title" -> assignmentName.value,
			"resource_link_description" -> assignmentName.value,
			"user_id" -> user.email,
			"context_id" -> classId.value,
			"context_title" -> className.value,
			"ext_resource_tool_placement_url" -> s"$topLevelUrl/api/turnitin-response",
			"ext_outcomes_tool_placement_url" -> s"$topLevelUrl/api/tunitin-outcomes",
			"create_session" -> "1") {
			request =>
				request >:+ {
					(headers, request) =>
						logger.info(headers.toString)
						request <> {
							(node) => TurnitinLtiResponse.fromXml(node)
						}
				}
		}
	}
}

trait TurnitinInitialTesterCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ManageEmergencyMessage)
	}
}

trait TurnitinInitialTesterCommandState {
	// Bind variables
	var assignment: Assignment = _
}