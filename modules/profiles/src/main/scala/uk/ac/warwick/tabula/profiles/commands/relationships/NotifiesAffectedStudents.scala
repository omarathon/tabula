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

trait NotifiesAffectedStudents extends Notifies[Seq[StudentRelationshipChange], Seq[StudentRelationship]] {
	this: RelationshipChangingCommand =>
		
	var notifyStudent: Boolean = false
	var notifyOldAgents: Boolean = false
	var notifyNewAgent: Boolean = false

	def emit(relationshipChanges: Seq[StudentRelationshipChange]): Seq[Notification[StudentRelationship, Unit]] = {
		val studentNotifications = if (notifyStudent) {
			relationshipChanges.flatMap {
				change =>
					change.modifiedRelationship.studentMember.map { student =>
						val notification = Notification.init(new BulkStudentRelationshipNotification, apparentUser, change.modifiedRelationship)
						notification.profileService = profileService // the auto-wired version is no good for testing
						notification.oldAgentIds.value = change.oldAgents.map(_.universityId)
						notification
					}
			}
		} else Nil

		val oldAgentNotifications = if (notifyOldAgents) {
		// We've got a sequence of modified relationships, each with a seq of old tutors.
		// We need to group by old tutors, not by sets of old tutors - so first the
		// changes are expanded so there's one for each oldAgent/modified relationship combination.
			val oldAgentAndRelSeq = (for (change <- relationshipChanges) yield {
				for (oldAgent <- change.oldAgents) yield {
					(oldAgent, change.modifiedRelationship)
				}
			}).flatten

			val oldAgentAndRelWithDupsRemoved = oldAgentAndRelSeq.filterNot(elem => oldAgentAndRelSeq.contains(elem))


			oldAgentAndRelWithDupsRemoved
				.groupBy(_._1)
				.map { case (oldAgent: Member, changes) =>
				val relationships = changes.map { _._2 }
				val notification = Notification.init(new BulkOldAgentRelationshipNotification, apparentUser, relationships)
				notification.profileService = profileService // the auto-wired version is no good for testing
				notification.oldAgentIds.value = Seq(oldAgent.universityId)
				notification
			}

		} else Nil

		val newAgentNotifications = if (notifyNewAgent) {
			relationshipChanges
				.groupBy(_.modifiedRelationship.agent)
				.filter { case (agent, changes) => agent.forall(_.isDigit) }
				.flatMap { case (agent, changes) => profileService.getMemberByUniversityId(agent) map { (_, changes) } }
				.map { case (agent, changes) =>
					val relationships = changes.map { _.modifiedRelationship }.filter(_.endDate == null) // TAB-2486
					Notification.init(new BulkNewAgentRelationshipNotification, apparentUser, relationships)

					// can't set old agents as each tutee for the new agent will have a different set
				}
		} else Nil

		studentNotifications ++ oldAgentNotifications ++ newAgentNotifications
	}
}
