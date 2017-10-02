package uk.ac.warwick.tabula.commands.profiles.relationships

import uk.ac.warwick.tabula.commands.Notifies
import uk.ac.warwick.tabula.data.model.{Notification, StudentMember, StudentRelationship}
import uk.ac.warwick.tabula.data.model.notifications.profiles.{BulkNewAgentRelationshipNotification, BulkOldAgentRelationshipNotification, BulkRelationshipChangeNotification, BulkStudentRelationshipNotification}

trait BulkRelationshipChangeNotifier[A, B] extends Notifies[A, B] {

	self: ManageStudentRelationshipsState with ManageStudentRelationshipsRequest =>

	def sharedEmit(expiredRelationships: Seq[StudentRelationship], addedRelationships: Seq[StudentRelationship]): Seq[BulkRelationshipChangeNotification] = {
		val studentNotifications = {
			if (notifyStudent) {
				val removalsByStudent = expiredRelationships.groupBy(_.studentMember)
				val additionsByStudent = addedRelationships.groupBy(_.studentMember)
				val allStudents: Set[StudentMember] = (removalsByStudent.keySet ++ additionsByStudent.keySet).flatten
				allStudents.map { student =>
					val recipient = additionsByStudent.getOrElse(Some(student), removalsByStudent.getOrElse(Some(student),Seq()))
					val notification = Notification.init(new BulkStudentRelationshipNotification, user.apparentUser, recipient)
					notification.oldAgentIds.value = removalsByStudent.getOrElse(Some(student), Seq()).map(_.agent)
					notification.newAgentIds.value = additionsByStudent.getOrElse(Some(student), Seq()).map(_.agent)
					if (scheduledDateToUse.isAfterNow) notification.scheduledDate = scheduledDateToUse
					if (previouslyScheduledDate.nonEmpty) notification.previouslyScheduledDate = previouslyScheduledDate.get
					notification
				}
			} else {
				Nil
			}
		}.toSeq

		val newAgentNotifications = if (notifyNewAgent) {
			addedRelationships.groupBy(_.agent).flatMap { case (_, relationships) =>
				relationships.head.agentMember.map { _ =>
					val notification = Notification.init(new BulkNewAgentRelationshipNotification, user.apparentUser, relationships)
					if (scheduledDateToUse.isAfterNow) notification.scheduledDate = scheduledDateToUse
					if (previouslyScheduledDate.nonEmpty) notification.previouslyScheduledDate = previouslyScheduledDate.get
					notification
				}
			}
		} else {
			Nil
		}

		val oldAgentNotifications = if (notifyOldAgent) {
			expiredRelationships.groupBy(_.agent).flatMap { case (_, relationships) =>
				relationships.head.agentMember.map { _ =>
					val notification = Notification.init(new BulkOldAgentRelationshipNotification, user.apparentUser, relationships)
					notification.oldAgentIds.value = relationships.map(_.agent)
					if (scheduledDateToUse.isAfterNow) notification.scheduledDate = scheduledDateToUse
					if (previouslyScheduledDate.nonEmpty) notification.previouslyScheduledDate = previouslyScheduledDate.get
					notification
				}
			}
		} else {
			Nil
		}

		studentNotifications ++ oldAgentNotifications ++ newAgentNotifications
	}

}
