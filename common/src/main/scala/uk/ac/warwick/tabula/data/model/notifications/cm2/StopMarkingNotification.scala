package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object StopMarkingNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/stop_marking_notification.ftl"
}

@Entity
@DiscriminatorValue("CM2StopMarking")
class StopMarkingNotification
	extends NotificationWithTarget[MarkerFeedback, Assignment]
		with MyWarwickNotification
		with SingleRecipientNotification
		with UserIdRecipientNotification
		with AutowiringUserLookupComponent
		with Logging {

	def verb = "stopped"
	def assignment: Assignment = target.entity

	def title: String = "%s: Marking has been stopped for \"%s\"".format(assignment.module.code.toUpperCase, assignment.name)

	def content = FreemarkerModel(StopMarkingNotification.templateLocation,
		Map(
			"assignment" -> assignment,
			"numStoppedFeedbacks" -> items.size
		))
	def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)
	def urlTitle = s"view any outstanding marking"

	priority = Warning

}


