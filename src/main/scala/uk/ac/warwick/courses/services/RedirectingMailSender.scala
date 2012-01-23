package uk.ac.warwick.courses.services

import java.lang.Boolean
import java.util.concurrent.Future

import org.springframework.mail.SimpleMailMessage

import javax.mail.internet.MimeMessage
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.util.concurrency.ImmediateFuture
import uk.ac.warwick.util.mail.WarwickMailSender
import collection.JavaConversions._

final class RedirectingMailSender(delegate:WarwickMailSender) extends WarwickMailSender with Logging {

  override def createMimeMessage() = delegate.createMimeMessage()

  override def send(message: MimeMessage): Future[Boolean] = {
    throw new UnsupportedOperationException()
  }
  
  implicit def ArrayOrEmpty[T:Manifest](a:Array[T]) = new {
	  def orEmpty:Array[T] = Option(a).getOrElse(Array.empty)
  }
  
  override def send(message: SimpleMailMessage): Future[Boolean] = {
	  val oldTo = message.getTo.mkString(", ")
	  message.setTo(Array("n.howes@warwick.ac.uk"))
	  message.setBcc(null:Array[String])
	  message.setCc(null:Array[String])
	  message.setText("This is a copy of a message that isn't being sent to the real recipients ("+oldTo+")  " +
	  		"because it is being sent from a non-production server.\n\n-------\n\n" 
	 		  + message.getText())
	  delegate.send(message)
//    logger.info("""-- Mock mail message --
//From: %s
//To: %s
//CC: %s
//
//%s
//    """ format (
//    		message.getFrom(),
//    		message.getTo().orEmpty.mkString(", "),
//    		message.getCc().orEmpty.mkString(", "),
//    		message.getText()
//    ))
//    ImmediateFuture.of(true)
  }
  


}