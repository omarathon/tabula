package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model._
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
	with SingleRecipientNotification
	with AutowiringUserLookupComponent {

	def markerFeedback = item.entity
	def parentFeedback = markerFeedback.feedback
	def rejectionFeedback = parentFeedback.secondMarkerFeedback
	def rejectedFeedback = parentFeedback.firstMarkerFeedback
	def actionRequired = true

	def assignment = markerFeedback.feedback.assignment

	def verb = "Released"

	def title: String = s"Feedback rejected by the moderator for ${assignment.module.code.toUpperCase} - ${assignment.name}"

	def content = FreemarkerModel(ModeratorRejectedNotification.templateLocation,
		Map(
			"moderatorName" -> agent.getFullName,
			"studentId" -> parentFeedback.universityId,
			"assignment" -> assignment,
			"rejectionComments" -> rejectionFeedback.rejectionComments,
			"adjustedMark" -> rejectionFeedback.mark,
			"adjustedGrade" -> rejectionFeedback.grade
		))

	def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)
	def urlTitle = "update the feedback and submit it for moderation again"

	// the recepient is the first marker
	def recipient = rejectedFeedback.getMarkerUsercode.map(userId => userLookup.getUserByUserId(userId))
		.getOrElse(throw new ItemNotFoundException(s"The recipient doesn't exist"))
}