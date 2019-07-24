package uk.ac.warwick.tabula.system

import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringCsrfServiceComponent, CsrfEnforceMessage}
import uk.ac.warwick.util.queue.conversion.ItemType
import uk.ac.warwick.util.queue.{Queue, QueueListener}

@Component
class CsrfEnforceListener extends QueueListener with InitializingBean with Logging with AutowiringCsrfServiceComponent {

  var queue: Queue = Wire.named[Queue]("settingsSyncTopic")

  override def isListeningToQueue = true

  override def onReceive(item: Any) {
    item match {
      case copy: CsrfEnforceMessage => csrfService.enforce = copy.enforce
      case _ =>
    }
  }

  override def afterPropertiesSet(): Unit = {
    queue.addListener(classOf[CsrfEnforceMessage].getAnnotation(classOf[ItemType]).value, this)
  }

}




