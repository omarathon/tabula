package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.notifications.EmailNotificationListener

@Service
class NotificationService extends Logging {

	type NotificationListener = Notification[_] => Unit

	final val emailListener = new EmailNotificationListener
	val listeners: List[NotificationListener] = List(emailListener.listen)

	def push(notification: Notification[_]){
		// TODO - In future pushing a notification will add it to a queue, aggregate similar notifications etc.
		logger.info("Notification pushed - " + notification)
		this.notify(notification) // for now we just hard call notify
	}

	def notify[A](notification: Notification[A]) {
		logger.info("Notify listeners - " + notification)
		for (l <- listeners) l(notification)
	}
}