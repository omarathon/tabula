package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

import scala.collection.JavaConverters._

object ReleaseToMarkerNoSubmissionsNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/released_to_marker_no_submissions_notification.ftl"
}

@Entity
@DiscriminatorValue("CM2ReleaseToMarkerNoSubmissions")
class ReleaseToMarkerNoSubmissionsNotification
	extends NotificationWithTarget[MarkerFeedback, Assignment]
		with SingleRecipientNotification
		with UserIdRecipientNotification
		with AutowiringUserLookupComponent
		with Logging
		with AllCompletedActionRequiredNotification {

	def workflowVerb: String = items.asScala.headOption.map(_.entity.stage.verb).getOrElse(MarkingWorkflowStage.DefaultVerb)

	def verb = "released"
	def assignment: Assignment = target.entity

	def title: String = "%s: \"%s\" released for marking".format(assignment.module.code.toUpperCase, assignment.name)

	def content = FreemarkerModel(ReleaseToMarkerNoSubmissionsNotification.templateLocation,
		Map(
			"assignment" -> assignment,
			"numReleasedFeedbacks" -> items.size,
			"workflowVerb" -> workflowVerb
		))
	def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)
	def urlTitle = s"$workflowVerb these submissions"

	priority = Warning

}