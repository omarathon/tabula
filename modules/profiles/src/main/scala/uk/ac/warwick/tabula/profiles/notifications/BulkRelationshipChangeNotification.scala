package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.SingleRecipientNotification
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.profiles.commands.relationships.StudentRelationshipChange
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

abstract class BulkRelationshipChangeNotification (
	private val relationshipType: StudentRelationshipType, 
	private val changes: Seq[StudentRelationshipChange],
	val agent: User,
	val recipient: User,
	private val templateLocation: String
) extends Notification[Seq[StudentRelationshipChange]] with SingleRecipientNotification {
	
	this: TextRenderer =>
		
	private val students = changes.flatMap(_.modifiedRelationship.studentMember)
		
	val verb: String = "change"
	val _object = changes
	val target: Option[AnyRef] = Some(students)
	
	def title: String = relationshipType.description + " change"
	def content: String = {
		renderTemplate(templateLocation, Map(
			"changes" -> changes,
			"path" -> url
		) ++ extraModel)
	}
	
	def extraModel: Map[String, Any]
	def url: String

}

class BulkStudentRelationshipNotification (
	relationshipType: StudentRelationshipType, 
	change: StudentRelationshipChange, 
	agent: User, 
	recipient: User, 
	private val student: Member
) extends BulkRelationshipChangeNotification(relationshipType, Seq(change), agent, recipient, BulkRelationshipChangeNotification.StudentTemplate) {
	
	this: TextRenderer =>
		
	val newAgent = 
		if (change.modifiedRelationship.endDate != null && change.modifiedRelationship.endDate.isBeforeNow) None
		else change.modifiedRelationship.agentMember
		
	def extraModel = Map(
		"student" -> student,
		"oldAgent" -> change.oldAgent,
		"newAgent" -> newAgent
	)
		
	def url: String = Routes.profile.view(student)
	
}

class BulkNewAgentRelationshipNotification (
	relationshipType: StudentRelationshipType, 
	changes: Seq[StudentRelationshipChange],
	agent: User, 
	recipient: User,
	private val newAgent: Member
) extends BulkRelationshipChangeNotification(relationshipType, changes, agent, recipient, BulkRelationshipChangeNotification.NewAgentTemplate) {
	
	this: TextRenderer =>
		
	def extraModel = Map(
		"newAgent" -> newAgent
	)
		
	def url: String = Routes.students(relationshipType)
	
}

class BulkOldAgentRelationshipNotification (
	relationshipType: StudentRelationshipType, 
	changes: Seq[StudentRelationshipChange],
	agent: User, 
	recipient: User,
	private val oldAgent: Member
) extends BulkRelationshipChangeNotification(relationshipType, changes, agent, recipient, BulkRelationshipChangeNotification.OldAgentTemplate) {
	
	this: TextRenderer =>
		
	def extraModel = Map(
		"oldAgent" -> oldAgent
	)
		
	def url: String = Routes.students(relationshipType)
	
}

object BulkRelationshipChangeNotification {
	val NewAgentTemplate = "/WEB-INF/freemarker/notifications/bulk_new_agent_notification.ftl"
	val OldAgentTemplate = "/WEB-INF/freemarker/notifications/bulk_old_agent_notification.ftl"
	val StudentTemplate = "/WEB-INF/freemarker/notifications/student_change_relationship_notification.ftl"
}