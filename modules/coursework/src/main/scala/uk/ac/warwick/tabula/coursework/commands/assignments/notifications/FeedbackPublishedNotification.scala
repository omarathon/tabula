package uk.ac.warwick.tabula.coursework.commands.assignments.notifications

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, Notification, Feedback}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.web.Routes

class FeedbackPublishedNotification (feedback: Feedback, currentUser: User, student: User)
	extends Notification[Feedback] with SingleRecipientNotification {

	this: TextRenderer =>

	val assignment = feedback.assignment
	val module = assignment.module
	val moduleCode = module.code.toUpperCase

	val agent = currentUser
	val verb = "publish"
	val _object = feedback
	val target = Some(assignment)

	def title = moduleCode + ": Your coursework feedback is ready"

	def content = renderTemplate("/WEB-INF/freemarker/emails/feedbackready.ftl", Map(
		"assignmentName" -> assignment.name,
		"moduleCode" -> assignment.module.code.toUpperCase,
		"moduleName" -> assignment.module.name,
		"path" -> url
	))

	def url = Routes.assignment.receipt(assignment)

	def recipient = student
}
