package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{Notification, ScheduledNotification}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.ScheduledNotificationDao
import uk.ac.warwick.tabula.helpers.ReflectionHelper
import uk.ac.warwick.userlookup.AnonymousUser


trait ScheduledNotificationService {
	def removeInvalidNotifications(target: Any)
	def push(sn: ScheduledNotification[_])
	def processNotifications(sn: ScheduledNotification[_])
}

@Service
class ScheduledNotificationServiceImpl extends ScheduledNotificationService {

	var dao = Wire.auto[ScheduledNotificationDao]
	var notificationService = Wire.auto[NotificationService]

	override def push(sn: ScheduledNotification[_]) = dao.save(sn)

	override def removeInvalidNotifications(target: Any) = {
		val existingNotifications = dao.getScheduledNotifications(target)
		existingNotifications.foreach(dao.delete)
	}

	override def processNotifications(sn: ScheduledNotification[_]) = {
		for (sn <- dao.getNotificationsToComplete) {

			val notificationClass = ReflectionHelper.allNotifications(sn.notificationType)

			val notification = Notification.init(notificationClass.newInstance(), new AnonymousUser, sn.target.entity)
			notificationService.push(notification)

			sn.completed = true
			dao.save(sn)
		}
	}
}
