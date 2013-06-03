package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.helpers.Logging

@Service
class NotificationService extends Logging{

	type NotificationListener = (Notification[_]) => Unit

	val listeners: List[NotificationListener] = Nil

	def push(notification: Notification[_]){
		// TODO - In future pushing a notification will add it to a queue, aggregate similar notifications etc.
		logger.error("Notification pushed" + notification)
		this.notify(notification) // for now we just hard call notify
	}

	def notify[A](notification: Notification[A]) { for (l <- listeners) l(notification) }
}