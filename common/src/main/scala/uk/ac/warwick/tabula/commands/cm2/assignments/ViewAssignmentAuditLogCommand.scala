package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.cm2.{AssignmentAuditEvent, AssignmentAuditQueryServiceComponent, AutowiringAssignmentAuditQueryServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ViewAssignmentAuditLogCommand {
	def apply(assignment: Assignment): Appliable[Seq[AssignmentAuditEvent]] =
		new ViewAssignmentAuditLogCommandInternal(assignment)
			with ComposableCommand[Seq[AssignmentAuditEvent]]
			with ViewAssignmentAuditLogState
			with ViewAssignmentAuditLogPermissions
			with AutowiringAssignmentAuditQueryServiceComponent
			with Unaudited
}

abstract class ViewAssignmentAuditLogCommandInternal(val assignment: Assignment) extends CommandInternal[Seq[AssignmentAuditEvent]] {
	self: ViewAssignmentAuditLogState with AssignmentAuditQueryServiceComponent =>

	override def applyInternal(): Seq[AssignmentAuditEvent] =
		assignmentAuditQueryService.getAuditEventsForAssignment(assignment)
}

trait ViewAssignmentAuditLogState {
	def assignment: Assignment
}

trait ViewAssignmentAuditLogPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewAssignmentAuditLogState =>
	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Assignment.Read, mandatory(assignment))
	}
}
