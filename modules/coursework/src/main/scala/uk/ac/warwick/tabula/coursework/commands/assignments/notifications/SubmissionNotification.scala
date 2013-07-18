package uk.ac.warwick.tabula.coursework.commands.assignments.notifications

import uk.ac.warwick.tabula.data.model.{Submission, Notification}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.web.Routes

abstract class SubmissionNotification(submission:Submission, student:User)
	extends Notification[Submission]{

	this: TextRenderer =>

	val assignment = submission.assignment
	val module = assignment.module

	val agent = student
	val verb = "submit"
	val _object = submission
	val target = Some(submission.assignment)

	val moduleCode = module.code.toUpperCase

	val templateLocation : String

	def content = renderTemplate(templateLocation, Map(
		"submission" -> submission,
		"submissionDate" -> dateFormatter.print(submission.submittedDate),
		"assignment" -> assignment,
		"module" -> module,
		"user" -> student,
		"path" -> url)
	)

	def url = Routes.assignment.receipt(assignment)
}
