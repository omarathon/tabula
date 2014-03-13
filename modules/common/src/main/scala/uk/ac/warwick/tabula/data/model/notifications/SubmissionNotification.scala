package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, NotificationWithTarget, SingleItemNotification, Assignment, Submission}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent


abstract class SubmissionNotification
	extends NotificationWithTarget[Submission, Assignment]
	with SingleItemNotification[Submission]
	with AutowiringUserLookupComponent {

	def submission = item.entity
	def assignment = target.entity
	def module = assignment.module
	def moduleCode = module.code.toUpperCase

	def verb = "submit"
	def templateLocation : String

	def content = FreemarkerModel(templateLocation, Map(
		"submission" -> submission,
		"submissionDate" -> dateTimeFormatter.print(submission.submittedDate),
		"assignment" -> assignment,
		"module" -> module,
		"user" -> userLookup.getUserByWarwickUniId(submission.universityId),
		"path" -> url)
	)

	def url = Routes.assignment.receipt(assignment)
}
