package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.{ActionRequiredNotification, Assignment, NotificationWithTarget, OriginalityReport}


abstract class TurnitinReportNotification
	extends NotificationWithTarget[OriginalityReport, Assignment] with ActionRequiredNotification {

	def assignment = target.entity

	def verb = "request"
	def url = Routes.admin.assignment.submissionsandfeedback(assignment)
	def urlTitle = "view the Turnitin results"
	priority = Warning

}