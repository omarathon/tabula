package uk.ac.warwick.tabula.data.model.notifications.cm2

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, AutowiringUserLookupComponent, CM2MarkingWorkflowServiceComponent}

import scala.collection.JavaConverters._

object ReleaseToMarkerNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/released_to_marker_notification.ftl"
}

@Entity
@DiscriminatorValue("CM2ReleaseToMarker")
class ReleaseToMarkerNotification
	extends NotificationWithTarget[MarkerFeedback, Assignment]
		with SingleRecipientNotification
		with UserIdRecipientNotification
		with AutowiringUserLookupComponent
		with Logging
		with AutowiringCM2MarkingWorkflowServiceComponent
		with AllCompletedActionRequiredNotification {

	val helper: ReleaseToMarkerNotificationHelper = new ReleaseToMarkerNotificationHelper(assignment, recipient, cm2MarkingWorkflowService)

	def workflowVerb: String = items.asScala.headOption.map(_.entity.stage.verb).getOrElse(MarkingWorkflowStage.DefaultVerb)

	def verb = "released"

	def assignment: Assignment = target.entity

	def title: String = s"${assignment.module.code.toUpperCase}: ${assignment.name} has been released for marking"

	def content = FreemarkerModel(ReleaseToMarkerNotification.templateLocation,
		if (assignment.collectSubmissions) {
			Map(
				"assignment" -> assignment,
				"numAllocated" -> helper.studentsAllocatedToThisMarker.size,
				"studentsAtStagesCount" -> helper.studentsAtStagesCount,
				"numReleasedFeedbacks" -> items.size,
				"numReleasedSubmissionsFeedbacks" -> helper.submissionsCount,
				"numReleasedNoSubmissionsFeedbacks" -> (helper.studentsAllocatedToThisMarker.size - helper.submissionsCount),
				"workflowVerb" -> workflowVerb
			)
		} else {
			Map(
				"assignment" -> assignment,
				"numReleasedFeedbacks" -> items.size,
				"workflowVerb" -> workflowVerb
			)
		}
	)

	def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)

	def urlTitle = s"${workflowVerb} the assignment '${assignment.module.code.toUpperCase} - ${assignment.name}'"

	priority = Warning

}

