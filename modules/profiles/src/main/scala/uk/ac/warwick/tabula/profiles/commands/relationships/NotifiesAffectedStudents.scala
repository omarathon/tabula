package uk.ac.warwick.tabula.profiles.commands.relationships

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.Notifies
import uk.ac.warwick.tabula.data.model.{Member, StudentMember, Notification, Department, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.notifications.{BulkOldAgentRelationshipNotification, BulkNewAgentRelationshipNotification, BulkStudentRelationshipNotification}

trait RelationshipChangingCommand {
	val department: Department
	val relationshipType: StudentRelationshipType
	val apparentUser: User
	
	var service: RelationshipService
	var profileService: ProfileService
}

trait NotifiesAffectedStudents extends Notifies[Seq[StudentRelationshipChange], Seq[StudentRelationshipChange]] {
	this: RelationshipChangingCommand =>
		
	var notifyStudent: Boolean = false
	var notifyOldAgent: Boolean = false
	var notifyNewAgent: Boolean = false

	def emit(relationshipChanges: Seq[StudentRelationshipChange]): Seq[Notification[StudentRelationship, Unit]] = {
		val studentNotifications = if (notifyStudent) {
			relationshipChanges.flatMap { change =>
				change.modifiedRelationship.studentMember.map { student => 
					Notification.init(new BulkStudentRelationshipNotification, apparentUser, Seq(change.modifiedRelationship))
				}
			}
		} else Nil
		
		val oldAgentNotifications = if (notifyOldAgent) {
			relationshipChanges
				.filter(_.oldAgent.isDefined)
				.groupBy(_.oldAgent.get)
				.map { case (oldAgent, changes) =>
					val relationships = changes.map { _.modifiedRelationship }
					Notification.init(new BulkOldAgentRelationshipNotification, apparentUser, relationships)
				}
		} else Nil
		
		val newAgentNotifications = if (notifyNewAgent) {
			relationshipChanges
				.groupBy(_.modifiedRelationship.agent)
				.filter { case (agent, changes) => agent.forall(_.isDigit) }
				.flatMap { case (agent, changes) => profileService.getMemberByUniversityId(agent) map { (_, changes) } }
				.map { case (newAgent, changes) =>
					val relationships = changes.map { _.modifiedRelationship }
					Notification.init(new BulkNewAgentRelationshipNotification, apparentUser, relationships)
				}
		} else Nil

		studentNotifications ++ oldAgentNotifications ++ newAgentNotifications
	}
}
