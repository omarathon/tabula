package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.FreemarkerModel
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object ReleaseToMarkerNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/released_to_marker_notification.ftl"
}

@Entity
@DiscriminatorValue("ReleaseToMarker")
class ReleaseToMarkerNotification
	extends NotificationWithTarget[MarkerFeedback, Assignment]
	with SingleRecipientNotification
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent {

	def this(markerNumber: Int) {
		this()
		whichMarker.value = markerNumber
	}

	def titleVerb: Option[String] = whichMarker.value match {
		case 1 => Some(assignment.markingWorkflow.firstMarkerVerb)
		case 2 => assignment.markingWorkflow.secondMarkerVerb
	}

	@transient val whichMarker = IntSetting("marker", 1)

	def verb: String = "released"
	def assignment = target.entity

	def title: String = s"Feedback released for ${assignment.module.code.toUpperCase} - ${assignment.name}"
	def content = FreemarkerModel(ReleaseToMarkerNotification.templateLocation,
		Map(
			"assignment" -> assignment,
			"numReleasedFeedbacks" -> items.size
		))
	def url: String = Routes.admin.assignment.markerFeedback(assignment)
	def urlTitle = s"${titleVerb} this feedback"
}

