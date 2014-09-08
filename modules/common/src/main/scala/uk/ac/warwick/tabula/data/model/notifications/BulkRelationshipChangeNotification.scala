package uk.ac.warwick.tabula.data.model.notifications

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.spring.Wire
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.profiles.commands.relationships.StudentRelationshipChange

abstract class BulkRelationshipChangeNotification extends Notification[StudentRelationshipChange, Unit] {
	@transient val templateLocation: String

	def changes = items.asScala.map { reference => reference.entity}
	def relationshipType = changes(0).modifiedRelationship.relationshipType

	var relationshipService = Wire[RelationshipService]

	def verb: String = "change"

	def content = {
		FreemarkerModel(templateLocation, Map(
			"changes" -> changes,
			"relationshipType" -> relationshipType,
			"path" -> url
		) ++ extraModel)
	}

	def actionRequired = false


	def oldAgents = changes.flatMap( _.oldAgents)

	def extraModel: Map[String, Any]
}

@Entity
@DiscriminatorValue(value="BulkStudentRelationship")
class BulkStudentRelationshipNotification() extends BulkRelationshipChangeNotification with SingleItemNotification[StudentRelationshipChange] {
	@transient val templateLocation = BulkRelationshipChangeNotification.StudentTemplate

	def modifiedRelationship = item.entity.modifiedRelationship

	def title: String = s"${modifiedRelationship.relationshipType.agentRole.capitalize} allocation"

	def newAgent =
		if (modifiedRelationship.endDate != null && modifiedRelationship.endDate.isBeforeNow) None
		else modifiedRelationship.agentMember

	def recipients = modifiedRelationship.studentMember.map { _.asSsoUser }.toSeq
		
	def url: String = {
		val student = modifiedRelationship.studentMember.getOrElse(throw new IllegalStateException("No student"))
		Routes.profile.view(student)
	}

	def urlTitle: String = "view this information on your student profile"

	def extraModel = Map(
		"student" -> modifiedRelationship.studentMember,
		"oldAgents" -> oldAgents.map(_.universityId).mkString(" "),
		"newAgent" -> newAgent
	)
	
}

@Entity
@DiscriminatorValue(value="BulkNewAgentRelationship")
class BulkNewAgentRelationshipNotification extends BulkRelationshipChangeNotification {
	@transient val templateLocation = BulkRelationshipChangeNotification.NewAgentTemplate

	def newAgent = items.asScala.headOption.flatMap { _.entity.modifiedRelationship.agentMember }

	def title: String = s"Allocation of new ${relationshipType.studentRole}s"

	def recipients = newAgent.map { _.asSsoUser }.toSeq

	def url: String = Routes.students(relationshipType)

	def urlTitle: String = s"view all of your ${relationshipType.studentRole}s"

	def extraModel = Map(
		"newAgent" -> newAgent
	)

	// Doesn't make sense here as there could be multiple old agents
	override def oldAgents = throw new UnsupportedOperationException("No sensible value for new agent notification")
}

@Entity
@DiscriminatorValue(value="BulkOldAgentRelationship")
class BulkOldAgentRelationshipNotification extends BulkRelationshipChangeNotification{
	@transient val templateLocation = BulkRelationshipChangeNotification.OldAgentTemplate

	def title: String = s"Change to ${relationshipType.studentRole.capitalize}s"

	def recipients = oldAgents.map { _.asSsoUser }.toSeq

	def url: String = Routes.students(relationshipType)

	def urlTitle: String = s"view your ${relationshipType.studentRole}s"

	def extraModel = Map(
		"oldAgents" -> oldAgents
	)
}

object BulkRelationshipChangeNotification {
	val NewAgentTemplate = "/WEB-INF/freemarker/notifications/bulk_new_agent_notification.ftl"
	val OldAgentTemplate = "/WEB-INF/freemarker/notifications/bulk_old_agent_notification.ftl"
	val StudentTemplate = "/WEB-INF/freemarker/notifications/student_change_relationship_notification.ftl"
}