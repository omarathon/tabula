package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{ToEntityReference, Notification, ScheduledNotification}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.ScheduledNotificationDao
import uk.ac.warwick.tabula.helpers.ReflectionHelper
import uk.ac.warwick.userlookup.AnonymousUser


trait ScheduledNotificationService {
	def removeInvalidNotifications(target: Any)
	def push(sn: ScheduledNotification[_])
	def generateNotification(sn: ScheduledNotification[_ >: Null <: ToEntityReference]) : Notification[_,_]
	def processNotifications()
}

@Service
class ScheduledNotificationServiceImpl extends ScheduledNotificationService {

	var dao = Wire.auto[ScheduledNotificationDao]
	var notificationService = Wire.auto[NotificationService]

	// a map of DiscriminatorValue -> Notification
	val notificationMap = ReflectionHelper.allNotifications

	override def push(sn: ScheduledNotification[_]) = dao.save(sn)

	override def removeInvalidNotifications(target: Any) = {
		val existingNotifications = dao.getScheduledNotifications(target)
		existingNotifications.foreach(dao.delete)
	}

	override def generateNotification(sn: ScheduledNotification[_ >: Null <: ToEntityReference]) = {
		val notificationClass = notificationMap(sn.notificationType)
		val baseNotification: Notification[ToEntityReference, Unit] = notificationClass.newInstance()
		val entity: ToEntityReference = sn.target.entity
		Notification.init(baseNotification, new AnonymousUser, entity)
	}

	/**
	 * This is called peridoically to convert uncompleted ScheduledNotifications into real instances of notification.
	 */
	override def processNotifications() = {
		for (sn <- dao.notificationsToComplete) {
			val notification = generateNotification(sn)
			notificationService.push(notification)

			sn.completed = true
			dao.save(sn)
		}
	}
}
