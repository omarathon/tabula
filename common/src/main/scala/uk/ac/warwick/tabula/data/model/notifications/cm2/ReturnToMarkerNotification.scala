package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback, NotificationWithTarget, SingleRecipientNotification, UserIdRecipientNotification, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object ReturnToMarkerNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/return_to_marker_notification.ftl"
}

@Entity
@DiscriminatorValue("CM2ReturnToMarker")
class ReturnToMarkerNotification
	extends NotificationWithTarget[MarkerFeedback, Assignment]
	with SingleRecipientNotification
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent
	with Logging
	with AllCompletedActionRequiredNotification {

	def this(stage: MarkingWorkflowStage, commentValue: String) {
		this()
		whichMarker.value = stage.name
		comment.value = commentValue
	}

	def workflowVerb: String = MarkingWorkflowStage.fromCode(whichMarker.value).verb

	@transient val whichMarker = StringSetting("stage", "")
	@transient val comment = StringSetting("comment", "")

	def verb = "returned"
	def assignment: Assignment = target.entity

	def title = s"${assignment.module.code.toUpperCase}: Submissions for ${assignment.name} have been returned to you"

	def content = FreemarkerModel(ReturnToMarkerNotification.templateLocation,
		Map(
			"assignment" -> assignment,
			"numReleasedFeedbacks" -> items.size,
			"workflowVerb" -> workflowVerb,
			"comment" -> comment.value
		))
	def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)
	def urlTitle = s"$workflowVerb this feedback"

	priority = Warning

}

