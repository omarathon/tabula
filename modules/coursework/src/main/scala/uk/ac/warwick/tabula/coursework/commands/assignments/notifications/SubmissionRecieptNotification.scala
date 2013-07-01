package uk.ac.warwick.tabula.coursework.commands.assignments.notifications

import uk.ac.warwick.tabula.data.model.{Submission, Notification}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.web.Routes

class SubmissionRecieptNotification(submission:Submission, student:User)
	extends SubmissionNotification(submission, student){

	this: TextRenderer =>

	def title = moduleCode + ": Submission receipt"
	val templateLocation = "/WEB-INF/freemarker/emails/submissionreceipt.ftl"
	def recipients = Seq(student)
}
