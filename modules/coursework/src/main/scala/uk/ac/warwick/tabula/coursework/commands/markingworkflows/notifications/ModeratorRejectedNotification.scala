package uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications

import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, Notification, MarkerFeedback}
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.web.Routes

/**
 * Sent when a moderator rejects the markers feedback
 */

object ModeratorRejectedNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/moderator_rejected_noification.ftl"
}

class ModeratorRejectedNotification(val agent: User, val recipient: User, val _object: MarkerFeedback, val rejectionFeeback: MarkerFeedback)
	extends Notification[MarkerFeedback] with SingleRecipientNotification {

	this: TextRenderer =>

	import ModeratorRejectedNotification._

	val assignment = _object.feedback.assignment

	val verb: String = "Released"
	val target: Option[AnyRef] = Some(recipient)

	def title: String = s"Feedback rejected by the moderator for ${assignment.module.code.toUpperCase} - ${assignment.name}"

	def content: String = renderTemplate(templateLocation,
		Map(
			"moderatorName" -> agent.getFullName,
			"studentId" -> _object.feedback.universityId,
			"assignment" -> assignment,
			"markingUrl" -> url,
			"rejectionComments" -> rejectionFeeback.rejectionComments,
			"adjustedMark" -> rejectionFeeback.mark,
			"adjustedGrade" -> rejectionFeeback.grade
		))

	def url: String = Routes.admin.assignment.markerFeedback(assignment)

}