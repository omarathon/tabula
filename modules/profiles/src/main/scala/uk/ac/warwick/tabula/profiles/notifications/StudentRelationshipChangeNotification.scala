package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, Notification, StudentRelationship, Member}
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.userlookup.User

class StudentRelationshipChangeNotification (
	private val relationship:StudentRelationship,
	val agent:User,
	val recipient:User,
	private val oldAgent: Option[Member],
	private val templateLocation: String
) extends Notification[StudentRelationship] with SingleRecipientNotification {

	this: TextRenderer =>

	val verb:String = "change"
	val _object = relationship
	val target: Option[AnyRef] = relationship.studentMember

	val newAgent = if (relationship.endDate != null && relationship.endDate.isBeforeNow) {
		None
	} else {
		relationship.agentMember
	}

	def title: String = relationship.relationshipType.description + " change"
	def content: String = {
		renderTemplate(templateLocation, Map(
			"student" -> relationship.studentMember,
			"oldAgent" -> oldAgent,
			"newAgent" -> newAgent,
			"relationshipType" -> relationship.relationshipType,
			"path" -> url
		))
	}
	def url: String = Routes.profile.view(relationship.studentMember.get)
}

object StudentRelationshipChangeNotification {
	val NewAgentTemplate = "/WEB-INF/freemarker/notifications/new_agent_notification.ftl"
	val OldAgentTemplate = "/WEB-INF/freemarker/notifications/old_agent_notification.ftl"
	val StudentTemplate = "/WEB-INF/freemarker/notifications/student_change_relationship_notification.ftl"
}