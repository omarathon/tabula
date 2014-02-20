package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{OriginalityReport, NotificationWithTarget, Assignment}
import uk.ac.warwick.tabula.coursework.web.Routes


abstract class TurnitinReportNotification
	extends NotificationWithTarget[OriginalityReport, Assignment] {

	def assignment = target.entity

	def verb = "request"
	def url = Routes.admin.assignment.submissionsandfeedback(assignment)

}
