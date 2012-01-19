package uk.ac.warwick.courses.services

import java.lang.Boolean
import java.util.concurrent.Future

import org.springframework.mail.SimpleMailMessage

import javax.mail.internet.MimeMessage
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.util.concurrency.ImmediateFuture
import uk.ac.warwick.util.mail.WarwickMailSender

final class RedirectingMailSender(delegate:WarwickMailSender) extends WarwickMailSender with Logging {

  override def createMimeMessage() = delegate.createMimeMessage()

  override def send(message: MimeMessage): Future[Boolean] = {
    throw new UnsupportedOperationException()
  }
  
  override def send(message: SimpleMailMessage): Future[Boolean] = {
    logger.info("""-- Mock mail message --
From: %s
To: %s
CC: %s

%s
    """ format (
    		message.getFrom(),
    		message.getTo().mkString(", "),
    		message.getCc().mkString(", "),
    		message.getText()
    ))
    ImmediateFuture.of(true)
  }

}