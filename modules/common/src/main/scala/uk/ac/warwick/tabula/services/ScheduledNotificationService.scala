package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.ScheduledNotification
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.ScheduledNotificationDao

trait ScheduledNotificationService {
	def removeInvalidNotifications(target: Any)
	def push(sn: ScheduledNotification[_])
}

@Service
class ScheduledNotificationServiceImpl extends ScheduledNotificationService {

	var dao = Wire.auto[ScheduledNotificationDao]

	override def push(sn: ScheduledNotification[_]) = dao.save(sn)

	override def removeInvalidNotifications(target: Any) = {
		val existingNotifications = dao.getScheduledNotifications(target)
		existingNotifications.foreach(dao.delete)
	}
}
