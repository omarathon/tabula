package uk.ac.warwick.tabula.coursework.commands.assignments.notifications

import uk.ac.warwick.tabula.data.model.{Assignment, Notification}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.CurrentUser

class RequestAssignmentAccessNotification(assignment:Assignment, currentUser: CurrentUser, admins: Seq[User])
	extends Notification[Assignment] {

	this: TextRenderer =>

	val agent = currentUser.apparentUser
	val verb = "request"
	val _object = assignment
	val target = Some(assignment.module)

	def title = assignment.module.code.toUpperCase + ": Access request"

	def content = renderTemplate("/WEB-INF/freemarker/emails/requestassignmentaccess.ftl", Map(
		"assignment" -> assignment,
		"student" -> currentUser,
		"path" -> url)
	)

	def url = Routes.admin.assignment.edit(assignment)

	def recipients = admins
}
