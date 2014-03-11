package uk.ac.warwick.tabula.data.model.notifications

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.spring.Wire
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.profiles.commands.relationships.StudentRelationshipChange

abstract class BulkRelationshipChangeNotification
		extends Notification[StudentRelationship, Unit] {
	@transient val templateLocation: String

	def relationshipType = items.get(0).entity.relationshipType

	var relationshipService = Wire[RelationshipService]

	def verb: String = "change"
	def title: String = relationshipType.description + " change"
	def content = {
		FreemarkerModel(templateLocation, Map(
			"changes" -> items.asScala.map { reference =>
				val relationship = reference.entity
				val previousRelationship = relationshipService.getPreviousRelationship(relationship)
				StudentRelationshipChange(previousRelationship.flatMap{_.agentMember}, relationship)
			},
			"relationshipType" -> relationshipType,
			"path" -> url
		) ++ extraModel)
	}

	def oldAgent = {
		val rel = items.asScala.headOption.map{ _.entity }
		val previousRelationship = rel.flatMap { rel => relationshipService.getPreviousRelationship(rel) }
		previousRelationship.flatMap{ _.agentMember }
	}

	def extraModel: Map[String, Any]
}

@Entity
@DiscriminatorValue(value="BulkStudentRelationship")
class BulkStudentRelationshipNotification() extends BulkRelationshipChangeNotification with SingleItemNotification[StudentRelationship] {
	@transient val templateLocation = BulkRelationshipChangeNotification.StudentTemplate

	def modifiedRelationship = item.entity
		
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
		"oldAgent" -> oldAgent,
		"newAgent" -> modifiedRelationship.agentMember
	)
	
}

@Entity
@DiscriminatorValue(value="BulkNewAgentRelationship")
class BulkNewAgentRelationshipNotification extends BulkRelationshipChangeNotification {
	@transient val templateLocation = BulkRelationshipChangeNotification.NewAgentTemplate

	def newAgent = items.asScala.headOption.flatMap { _.entity.agentMember }

	def recipients = newAgent.map { _.asSsoUser }.toSeq

	def url: String = Routes.students(relationshipType)

	def urlTitle: String = s"view all of your ${relationshipType.studentRole}s"

	def extraModel = Map(
		"newAgent" -> newAgent
	)

	// Doesn't make sense here as there could be multiple old agents
	override def oldAgent = throw new UnsupportedOperationException("No sensible value for new agent notification")
}

@Entity
@DiscriminatorValue(value="BulkOldAgentRelationship")
class BulkOldAgentRelationshipNotification extends BulkRelationshipChangeNotification{
	@transient val templateLocation = BulkRelationshipChangeNotification.OldAgentTemplate

	def recipients = oldAgent.map { _.asSsoUser }.toSeq
		
	def url: String = Routes.students(relationshipType)

	def urlTitle: String = s"view all of your ${relationshipType.studentRole}s"

	def extraModel = Map(
		"oldAgent" -> oldAgent
	)
}

object BulkRelationshipChangeNotification {
	val NewAgentTemplate = "/WEB-INF/freemarker/notifications/bulk_new_agent_notification.ftl"
	val OldAgentTemplate = "/WEB-INF/freemarker/notifications/bulk_old_agent_notification.ftl"
	val StudentTemplate = "/WEB-INF/freemarker/notifications/student_change_relationship_notification.ftl"
}