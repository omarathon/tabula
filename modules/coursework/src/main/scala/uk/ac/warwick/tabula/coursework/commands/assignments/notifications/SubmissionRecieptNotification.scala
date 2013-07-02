package uk.ac.warwick.tabula.coursework.commands.assignments.notifications

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, Submission, Notification}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.web.Routes

class SubmissionRecieptNotification(submission:Submission, student:User)
	extends SubmissionNotification(submission, student) with SingleRecipientNotification {

	this: TextRenderer =>

	def title = moduleCode + ": Submission receipt"
	val templateLocation = "/WEB-INF/freemarker/emails/submissionreceipt.ftl"
	def recipient = student
}
