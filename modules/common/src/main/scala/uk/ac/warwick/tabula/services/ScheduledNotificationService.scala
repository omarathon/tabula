package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{ToEntityReference, Notification, ScheduledNotification}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.ScheduledNotificationDao
import uk.ac.warwick.tabula.helpers.{Logging, ReflectionHelper}
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.data.Transactions._
import org.hibernate.ObjectNotFoundException


trait ScheduledNotificationService {
	def removeInvalidNotifications(target: Any)
	def push(sn: ScheduledNotification[_])
	def generateNotification(sn: ScheduledNotification[_ >: Null <: ToEntityReference]) : Option[Notification[_,_]]
	def processNotifications()
}

@Service
class ScheduledNotificationServiceImpl extends ScheduledNotificationService with Logging {

	var dao = Wire.auto[ScheduledNotificationDao]
	var notificationService = Wire.auto[NotificationService]

	// a map of DiscriminatorValue -> Notification
	val notificationMap = ReflectionHelper.allNotifications

	override def push(sn: ScheduledNotification[_]) = dao.save(sn)

	override def removeInvalidNotifications(target: Any) = {
		val existingNotifications = dao.getScheduledNotifications(target)
		existingNotifications.foreach(dao.delete)
	}

	override def generateNotification(sn: ScheduledNotification[_ >: Null <: ToEntityReference]): Option[Notification[ToEntityReference, Unit]] = {
		try {
			val notificationClass = notificationMap(sn.notificationType)
			val baseNotification: Notification[ToEntityReference, Unit] = notificationClass.newInstance()
			val entity: ToEntityReference = sn.target.entity
			Some(Notification.init(baseNotification, new AnonymousUser, entity))
		} catch {
			// Can happen if reference to an entity has since been deleted, e.g.
			// a submission is resubmitted and the old submission is removed. Skip this notification.
			case onf: ObjectNotFoundException =>
				debug("Skipping scheduled notification %s as a referenced object was not found", sn)
				None
		}
	}

	/**
	 * This is called peridoically to convert uncompleted ScheduledNotifications into real instances of notification.
	 */
	override def processNotifications() = transactional() {
		dao.notificationsToComplete.foreach { sn =>
			generateNotification(sn).foreach(notificationService.push)

			// Even if we threw an error above and didn't actually push a notification, still mark it as completed
			sn.completed = true
			dao.save(sn)
		}
	}
}
