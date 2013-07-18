package uk.ac.warwick.tabula.coursework.jobs.notifications

import uk.ac.warwick.tabula.data.model.{Notification, Assignment}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.services.turnitin.TurnitinSubmissionInfo
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.web.Routes


abstract class TurnitinReportNotification(reports: Seq[TurnitinSubmissionInfo], val assignment: Assignment, val user: User)
	extends Notification[Seq[TurnitinSubmissionInfo]] {

	this: TextRenderer =>

	val agent = user
	val verb = "request"
	val _object = reports
	val target = Some(assignment)

	def recipients = Seq(user)
	def url = Routes.admin.assignment.submissionsandfeedback(assignment)

}
