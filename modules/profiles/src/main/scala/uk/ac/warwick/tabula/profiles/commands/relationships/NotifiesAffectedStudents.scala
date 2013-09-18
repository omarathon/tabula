package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.commands.Notifies
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.profiles.notifications.StudentRelationshipChangeNotification
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.tabula.profiles.notifications._
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

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

	def emit(relationshipChanges: Seq[StudentRelationshipChange]): Seq[Notification[Seq[StudentRelationshipChange]]] = {
		val studentNotifications = if (notifyStudent) {
			relationshipChanges.flatMap { change =>
				change.modifiedRelationship.studentMember.map { student => 
					val recipient = student.asSsoUser
					new BulkStudentRelationshipNotification(relationshipType, change, apparentUser, recipient, student) with FreemarkerTextRenderer
				}
			}
		} else Nil
		
		val oldAgentNotifications = if (notifyOldAgent) {
			relationshipChanges
				.filter(_.oldAgent.isDefined)
				.groupBy(_.oldAgent.get)
				.map { case (oldAgent, changes) =>
					val recipient = oldAgent.asSsoUser
					new BulkOldAgentRelationshipNotification(relationshipType, changes, apparentUser, recipient, oldAgent) with FreemarkerTextRenderer
				}
		} else Nil
		
		val newAgentNotifications = if (notifyNewAgent) {
			relationshipChanges
				.groupBy(_.modifiedRelationship.agent)
				.filter { case (agent, changes) => agent.forall(_.isDigit) }
				.flatMap { case (agent, changes) => profileService.getMemberByUniversityId(agent) map { (_, changes) } }
				.map { case (newAgent, changes) =>
					val recipient = newAgent.asSsoUser
					new BulkNewAgentRelationshipNotification(relationshipType, changes, apparentUser, recipient, newAgent) with FreemarkerTextRenderer
				}
		} else Nil

		studentNotifications ++ oldAgentNotifications ++ newAgentNotifications
	}
}
