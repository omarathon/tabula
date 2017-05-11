package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.RequestAssignmentAccessCommand._
import uk.ac.warwick.tabula.data.model.notifications.cm2.Cm2RequestAssignmentAccessNotification
import uk.ac.warwick.tabula.data.model.{Assignment, Notification}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.userlookup.User

/**
 * Sends a message to one or more admins to let them know that the current
 * user thinks they should have access to an assignment.
 */
object RequestAssignmentAccessCommand {
	type Result = Seq[User]
	type Command = Appliable[Result] with RequestAssignmentAccessCommandState

	def apply(assignment: Assignment, user: CurrentUser): Command =
		new RequestAssignmentAccessCommandInternal(assignment, user)
			with ComposableCommand[Result]
			with PubliclyVisiblePermissions
			with RequestAssignmentAccessNotifications
			with RequestAssignmentAccessDescription
}

trait RequestAssignmentAccessCommandState {
	def assignment: Assignment
	def user: CurrentUser

	// lookup the admin users - used to determine the recipients  for notifications
	lazy val admins: Seq[User] = assignment.module.adminDepartment.owners.users.filter(admin => admin.isFoundUser && admin.getEmail.hasText)
}

class RequestAssignmentAccessCommandInternal(val assignment: Assignment, val user: CurrentUser) extends CommandInternal[Result]
	with RequestAssignmentAccessCommandState {
	override def applyInternal(): Result = admins
}

trait RequestAssignmentAccessNotifications extends Notifies[Result, Assignment] {
	self: RequestAssignmentAccessCommandState with NotificationHandling =>

	override def emit(admins: Seq[User]): Seq[Cm2RequestAssignmentAccessNotification] = {
		Seq(Notification.init(new Cm2RequestAssignmentAccessNotification, user.apparentUser, Seq(assignment)))
	}
}

trait RequestAssignmentAccessDescription extends Describable[Result] {
	self: RequestAssignmentAccessCommandState =>

	override lazy val eventName: String = "RequestAssignmentAccess"

	override def describe(d: Description): Unit = d.assignment(assignment)
	override def describeResult(d: Description): Unit = d.property("admins" -> admins.flatMap(_.getUserId))
}