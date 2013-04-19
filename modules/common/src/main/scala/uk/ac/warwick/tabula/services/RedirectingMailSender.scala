package uk.ac.warwick.tabula.services

import java.util.concurrent.Future
import org.springframework.mail.SimpleMailMessage
import javax.mail.internet.MimeMessage
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.concurrency.ImmediateFuture
import uk.ac.warwick.util.mail.WarwickMailSender
import collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import javax.mail.Message.RecipientType
import javax.mail.internet.MimeMultipart
import scala.reflect.ClassTag
import language.implicitConversions

final class RedirectingMailSender(delegate: WarwickMailSender) extends WarwickMailSender with Logging with UnicodeEmails {

	var features = Wire[Features]

	var testEmailTo = Wire[String]("${redirect.test.emails.to}")

	override def createMimeMessage() = delegate.createMimeMessage()

	override def send(message: MimeMessage): Future[JBoolean] = {
		val messageToSend = if (!features.emailStudents) {
			prepareMessage(message) { helper =>
				val oldTo = message.getRecipients(RecipientType.TO).map({_.toString}).mkString(", ")
				
				helper.setTo(Array(testEmailTo))
				helper.setBcc(Array(): Array[String])
				helper.setCc(Array(): Array[String])
				
				val oldText = message.getContent match {
					case string: String => string
					case multipart: MimeMultipart => multipart.getBodyPart(0).getContent.toString
				}
				
				helper.setText("This is a copy of a message that isn't being sent to the real recipients (" + oldTo + ")  " +
											 "because it is being sent from a non-production server.\n\n-------\n\n"
											 + oldText)
			}
		} else message
		
		delegate.send(messageToSend)
	}

	implicit def ArrayOrEmpty[A: ClassTag](a: Array[A]) = new {
		def orEmpty: Array[A] = Option(a).getOrElse(Array.empty)
	}

	override def send(simpleMessage: SimpleMailMessage): Future[JBoolean] = send(createMessage(delegate) { message =>
		Option(simpleMessage.getFrom) map {message.setFrom(_)}
		Option(simpleMessage.getReplyTo) map {message.setReplyTo(_)}
		
		Option(simpleMessage.getTo) map {message.setTo(_)}
		message.setCc(Option(simpleMessage.getCc).getOrElse(Array(): Array[String]))
		message.setBcc(Option(simpleMessage.getBcc).getOrElse(Array(): Array[String]))
		
		Option(simpleMessage.getSubject) map {message.setSubject(_)}
		Option(simpleMessage.getText) map {message.setText(_)}
	})

}