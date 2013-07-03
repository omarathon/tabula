package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, Notification, StudentRelationship, Member}
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.userlookup.User

class TutorChangeNotification (
	private val relationship:StudentRelationship,
	val agent:User,
	private val _recipient:User,
	private val oldTutor: Option[Member],
	private val templateLocation: String
) extends Notification[StudentRelationship] with SingleRecipientNotification {

	this: TextRenderer =>

	val verb:String = "change"
	val _object = relationship
	val target: Option[AnyRef] = Some(relationship.studentMember)

	val newTutor = if(relationship.endDate != null && relationship.endDate.isBeforeNow){
		None
	} else {
		relationship.agentMember
	}

	def title: String = "Personal tutor change"
	def content: String = {
		renderTemplate(templateLocation, Map(
			"tutee" -> relationship.studentMember,
			"oldTutor" -> oldTutor,
			"newTutor" -> newTutor,
			"path" -> url
		))
	}
	def url: String = Routes.profile.view(relationship.studentMember)

	def recipient = _recipient
}

object TutorChangeNotification {
	val NewTutorTemplate = "/WEB-INF/freemarker/notifications/new_tutor_notification.ftl"
	val OldTutorTemplate = "/WEB-INF/freemarker/notifications/old_tutor_notification.ftl"
	val TuteeTemplate = "/WEB-INF/freemarker/notifications/tutor_change_tutee_notification.ftl"
}