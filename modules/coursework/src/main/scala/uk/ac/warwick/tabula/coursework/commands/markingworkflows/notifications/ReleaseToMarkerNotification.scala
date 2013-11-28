package uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications

import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback, SingleRecipientNotification, Notification}
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.web.Routes

object ReleaseToMarkerNotifcation {
	val templateLocation = "/WEB-INF/freemarker/emails/released_to_marker_notification.ftl"
}
class ReleaseToMarkerNotifcation(val agent: User, val recipient: User, val _object: Seq[MarkerFeedback], assignment: Assignment, isFirstMarker: Boolean)
	extends Notification[Seq[MarkerFeedback]] with SingleRecipientNotification {

	this: TextRenderer =>

	import ReleaseToMarkerNotifcation._

	val templateVerb: Option[String] = {
		if(isFirstMarker) {
			Some(assignment.markingWorkflow.firstMarkerVerb)
		} else {
			assignment.markingWorkflow.secondMarkerVerb
		}
	}

	val verb: String = "Released"
	val target: Option[AnyRef] = Some(recipient)

	def title: String = s"Feedback released for ${assignment.module.code.toUpperCase} - ${assignment.name}"

	def content: String = renderTemplate(templateLocation,
		Map(
			"markingUrl" -> url,
			"assignment" -> assignment,
			"numReleasedFeedbacks" -> _object.size,
			"verb" -> templateVerb
		))

	def url: String = Routes.admin.assignment.markerFeedback(assignment)
}