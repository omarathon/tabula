package uk.ac.warwick.tabula.events

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.logging.AuditLogger
import uk.ac.warwick.util.logging.AuditLogger.{Field, RequestInformation}
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.jdk.CollectionConverters._

/**
  * An event listener that sends events to CLogS using WarwickUtils' AuditLogger
  */
class AuditLoggingEventListener extends EventListener {

  val logger: AuditLogger = AuditLogger.getAuditLogger("tabula")

  override def beforeCommand(event: Event): Unit = {}

  override def onException(event: Event, exception: Throwable): Unit = {}

  // Currently we only want afters
  override def afterCommand(event: Event, returnValue: Any, beforeEvent: Event): Unit = {
    var info = RequestInformation.forEventType(event.name)
    event.realUserId.maybeText.foreach { realUserId => info = info.withUsername(realUserId) }
    event.userAgent.maybeText.foreach { userAgent => info = info.withUserAgent(userAgent) }
    event.ipAddress.maybeText.foreach { ipAddress => info = info.withIpAddress(ipAddress) }

    val data =
      (beforeEvent.extra ++ event.extra)
        .map { case (k, v) => new Field(k) -> Logging.convertForStructuredArguments(v) }
        .filterNot { case (_, v) => v == null }

    logger.log(info, data.asJava)
  }
}
