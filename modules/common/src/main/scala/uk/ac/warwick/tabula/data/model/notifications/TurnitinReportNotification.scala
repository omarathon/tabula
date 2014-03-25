package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{OriginalityReport, NotificationWithTarget, Assignment}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning


abstract class TurnitinReportNotification
	extends NotificationWithTarget[OriginalityReport, Assignment] {

	def assignment = target.entity

	def verb = "request"
	def url = Routes.admin.assignment.submissionsandfeedback(assignment)
	def urlTitle = "view the Turnitin results"
	priority = Warning
	def actionRequired = true
}