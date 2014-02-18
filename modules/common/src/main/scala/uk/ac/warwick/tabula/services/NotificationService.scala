package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.NotificationDao

@Service
class NotificationService extends Logging {

	val listeners = Wire.all[NotificationListener]
	var dao = Wire[NotificationDao]

	def push(notification: Notification[_,_]){
		// TODO - In future pushing a notification will add it to a queue, aggregate similar notifications etc.
		logger.info("Notification pushed - " + notification)
		dao.save(notification)
		this.notify(notification) // for now we just hard call notify
	}

	def notify[A](notification: Notification[_,_]) {
		logger.info("Notify listeners - " + notification)
		for (l <- listeners) l.listen(notification)
	}

}

trait NotificationListener {
	def listen(n: Notification[_,_]): Unit
}