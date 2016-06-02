package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

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
	with AutowiringUserLookupComponent
	with AllCompletedActionRequiredNotification {

	def markerFeedback = item.entity
	def parentFeedback = markerFeedback.feedback
	def rejectedFeedback = parentFeedback.firstMarkerFeedback

	def assignment = HibernateHelpers.initialiseAndUnproxy(parentFeedback) match {
		case assignmentFeedback: AssignmentFeedback => assignmentFeedback.assignment
		case _ => throw new IllegalArgumentException("Exam feedback used in Assignment notification")
	}

	def verb = "Released"

	def title = "%s: Feedback for %s for \"%s\" has been rejected by the moderator".format(assignment.module.code.toUpperCase, parentFeedback.universityId, assignment.name)

	def content = FreemarkerModel(ModeratorRejectedNotification.templateLocation,
		Map(
			"moderatorName" -> agent.getFullName,
			"studentId" -> parentFeedback.universityId,
			"assignment" -> assignment,
			"rejectionComments" -> markerFeedback.rejectionComments,
			"adjustedMark" -> markerFeedback.mark,
			"adjustedGrade" -> markerFeedback.grade
		))

	def url: String = Routes.admin.assignment.markerFeedback(assignment, recipients.head)
	def urlTitle = "update the feedback and submit it for moderation again"

	// the recepient is the first marker
	def recipients = rejectedFeedback.getMarkerUser.toSeq
}