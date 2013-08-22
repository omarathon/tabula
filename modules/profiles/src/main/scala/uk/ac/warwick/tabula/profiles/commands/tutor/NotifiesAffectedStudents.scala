package uk.ac.warwick.tabula.profiles.commands.tutor

import uk.ac.warwick.tabula.commands.Notifies
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.profiles.notifications.TutorChangeNotification
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.tabula.profiles.notifications._
import uk.ac.warwick.tabula.services.ProfileService

trait DepartmentCommand {
	val department: Department
	val apparentUser: User
	
	var service: RelationshipService
	var profileService: ProfileService
}

trait NotifiesAffectedStudents extends Notifies[Seq[PersonalTutorChange], Seq[PersonalTutorChange]] {
	this: DepartmentCommand =>
		
	var notifyTutee: Boolean = false
	var notifyOldTutor: Boolean = false
	var notifyNewTutor: Boolean = false

	def emit(tutorChanges: Seq[PersonalTutorChange]): Seq[Notification[Seq[PersonalTutorChange]]] = {
		val tuteeNotifications = if (notifyTutee) {
			tutorChanges.flatMap { change =>
				change.modifiedRelationship.studentMember.map { tutee => 
					val recipient = tutee.asSsoUser
					new BulkTuteeNotification(change, apparentUser, recipient, tutee) with FreemarkerTextRenderer
				}
			}
		} else Nil
		
		val oldTutorNotifications = if (notifyOldTutor) {
			tutorChanges
				.filter(_.oldTutor.isDefined)
				.groupBy(_.oldTutor.get)
				.map { case (oldTutor, changes) =>
					val recipient = oldTutor.asSsoUser
					new BulkOldTutorNotification(changes, apparentUser, recipient, oldTutor) with FreemarkerTextRenderer
				}
		} else Nil
		
		val newTutorNotifications = if (notifyNewTutor) {
			tutorChanges
				.groupBy(_.modifiedRelationship.agent)
				.filter { case (agent, changes) => agent.forall(_.isDigit) }
				.flatMap { case (agent, changes) => profileService.getMemberByUniversityId(agent) map { (_, changes) } }
				.map { case (newTutor, changes) =>
					val recipient = newTutor.asSsoUser
					new BulkNewTutorNotification(changes, apparentUser, recipient, newTutor) with FreemarkerTextRenderer
				}
		} else Nil

		tuteeNotifications ++ oldTutorNotifications ++ newTutorNotifications
	}
}
