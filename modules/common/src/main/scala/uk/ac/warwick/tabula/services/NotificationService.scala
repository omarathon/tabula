package uk.ac.warwick.tabula.services

import org.springframework.stereotype.{Component, Service}
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.notifications.EmailNotificationListener
import uk.ac.warwick.spring.Wire

@Service
class NotificationService extends Logging {

	val listeners = Wire.all[NotificationListener]

	def push(notification: Notification[_]){
		// TODO - In future pushing a notification will add it to a queue, aggregate similar notifications etc.
		logger.info("Notification pushed - " + notification)
		this.notify(notification) // for now we just hard call notify
	}

	def notify[A](notification: Notification[A]) {
		logger.info("Notify listeners - " + notification)
		for (l <- listeners) l.listen(notification)
	}
}

trait NotificationListener {
	def listen: Notification[_] => Unit
}