package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleItemNotification, Notification, MarkerFeedback}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import javax.persistence.{DiscriminatorValue, Entity}

/**
 * Sent when a moderator rejects the markers feedback
 */

object ModeratorRejectedNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/moderator_rejected_notification.ftl"
}

@Entity
@DiscriminatorValue(value="ModeratorRejected")
class ModeratorRejectedNotification extends Notification[MarkerFeedback, Unit]
	with SingleItemNotification[MarkerFeedback]
	with AutowiringUserLookupComponent {

	def markerFeedback = item.entity
	def parentFeedback = markerFeedback.feedback
	def rejectionFeeback = parentFeedback.secondMarkerFeedback

	def assignment = markerFeedback.feedback.assignment

	def verb = "Released"

	def title: String = s"Feedback rejected by the moderator for ${assignment.module.code.toUpperCase} - ${assignment.name}"

	def content = FreemarkerModel(ModeratorRejectedNotification.templateLocation,
		Map(
			"moderatorName" -> agent.getFullName,
			"studentId" -> parentFeedback.universityId,
			"assignment" -> assignment,
			"rejectionComments" -> rejectionFeeback.rejectionComments,
			"adjustedMark" -> rejectionFeeback.mark,
			"adjustedGrade" -> rejectionFeeback.grade
		))

	def url: String = Routes.admin.assignment.markerFeedback(assignment)
	def urlTitle = "update the feedback and submit it for moderation again"

	// the recepient is the first marker
	def recipients = markerFeedback.getMarkerUsercode.map(userId => userLookup.getUserByUserId(userId)).toSeq
}