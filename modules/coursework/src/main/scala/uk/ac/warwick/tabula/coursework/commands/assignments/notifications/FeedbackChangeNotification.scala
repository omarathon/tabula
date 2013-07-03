package uk.ac.warwick.tabula.coursework.commands.assignments.notifications

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, Feedback, Notification}
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.userlookup.User

class FeedbackChangeNotification(feedback: Feedback, currentUser: User, student: User) extends Notification[Feedback]
with SingleRecipientNotification {

	this: TextRenderer =>

	val assignment = feedback.assignment
	val module = assignment.module
	val moduleCode = module.code.toUpperCase

	val agent = currentUser
	val verb = "modify"
	val _object = feedback
	val target = Some(assignment)

	def title = moduleCode + ": Feedback updated"

	def content = renderTemplate("/WEB-INF/freemarker/emails/feedbackchanged.ftl", Map(
		"assignment" -> assignment,
		"module" -> module,
		"path" -> url
	))

	def url = Routes.assignment.receipt(assignment)

	val recipient = student
}
