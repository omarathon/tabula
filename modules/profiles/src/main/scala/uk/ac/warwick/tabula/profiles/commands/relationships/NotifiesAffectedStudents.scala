package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.commands.Notifies
import uk.ac.warwick.tabula.data.model.notifications.profiles.{BulkOldAgentRelationshipNotification, BulkNewAgentRelationshipNotification, BulkStudentRelationshipNotification}
import uk.ac.warwick.tabula.data.model.{Member, Notification, Department, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.services.ProfileService

trait RelationshipChangingCommand {
	val department: Department
	val relationshipType: StudentRelationshipType
	val apparentUser: User

	var relationshipService: RelationshipService
	var profileService: ProfileService
}

trait NotifiesAffectedStudents extends Notifies[Seq[StudentRelationshipChange], Seq[StudentRelationship]] {
	this: RelationshipChangingCommand =>

	var notifyStudent: Boolean = false
	var notifyOldAgents: Boolean = false
	var notifyNewAgent: Boolean = false

	def emit(relationshipChanges: Seq[StudentRelationshipChange]): Seq[Notification[StudentRelationship, Unit]] = {

		val studentNotifications = if (notifyStudent) {
			relationshipChanges
				.groupBy(_.modifiedRelationship.studentCourseDetails.student)
				.map { case (student, changes) =>
					val relationships = changes.map(_.modifiedRelationship)
					val notification = Notification.init(new BulkStudentRelationshipNotification, apparentUser, relationships)
					notification.profileService = profileService // the auto-wired version is no good for testing
					notification.oldAgentIds.value = changes.flatMap(_.oldAgents).distinct.map(_.universityId)
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
				val notification = Notification.init(new BulkNewAgentRelationshipNotification, apparentUser, relationships)
				notification.profileService = profileService // the auto-wired version is no good for testing
				notification
				// can't set old agents as each tutee for the new agent will have a different set
			}
		} else Nil

		val oldAgentNotifications = if (notifyOldAgents) {
		// We've got a sequence of modified relationships, each with a seq of old tutors.
		// We need to group by old tutors, not by sets of old tutors - so first the
		// changes are expanded so there's one for each oldAgent/modified relationship combination.

			val oldAgentAndRelSeq = for (
				change <- relationshipChanges;
				oldAgent <- change.oldAgents
			) yield (oldAgent, change.modifiedRelationship)


			oldAgentAndRelSeq
				.groupBy(_._1) // group by old agent
				.map { case (oldAgent, changes) =>
				val relationships = changes.map { _._2 }
				val notification = Notification.init(new BulkOldAgentRelationshipNotification, apparentUser, relationships)
				notification.profileService = profileService // the auto-wired version is no good for testing
				notification.oldAgentIds.value = Seq(oldAgent.universityId)
				notification
			}

		} else Nil

		studentNotifications.toSeq ++ oldAgentNotifications ++ newAgentNotifications
	}
}
