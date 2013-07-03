package uk.ac.warwick.tabula.coursework.commands.assignments.notifications

import uk.ac.warwick.tabula.data.model.{Submission, Notification}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.web.Routes

class SubmissionRecievedNotification(submission:Submission, student:User, admins:Seq[User])
	extends SubmissionNotification(submission, student){

	this: TextRenderer =>

	val submissionTitle =
		if(submission.isAuthorisedLate) "Authorised Late Submission"
		else if(submission.isLate) "Late Submission"
		else "Submission"

	def title = moduleCode + ": " + submissionTitle
	val templateLocation = "/WEB-INF/freemarker/emails/submissionnotify.ftl"
	def recipients = admins
}
