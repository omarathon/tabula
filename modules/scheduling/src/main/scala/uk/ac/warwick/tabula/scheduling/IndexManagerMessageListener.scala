package uk.ac.warwick.tabula.scheduling

import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{IndexNotificationRecipientsMessage, IndexManager}
import uk.ac.warwick.util.queue.conversion.ItemType
import uk.ac.warwick.util.queue.{Queue, QueueListener}

class IndexManagerMessageListener extends QueueListener with InitializingBean with Logging {

	var queue = Wire.named[Queue]("indexTopic")
	var indexManager = Wire[IndexManager]
	var context = Wire.property("${module.context}")

	override def isListeningToQueue = true
	override def onReceive(item: Any) {
		logger.info("Synchronising item " + item + " for " + context)
		item match {
			case message: IndexNotificationRecipientsMessage => indexManager.indexNotificationRecipients(message)
			case _ => // Should never happen
		}
	}

	override def afterPropertiesSet() {
		logger.info("Registering listener for " + classOf[IndexNotificationRecipientsMessage].getAnnotation(classOf[ItemType]).value + " on " + context)
		queue.addListener(classOf[IndexNotificationRecipientsMessage].getAnnotation(classOf[ItemType]).value, this)
	}

}
