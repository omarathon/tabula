package uk.ac.warwick.tabula.helpers

import org.springframework.mail.javamail.MimeMessagePreparator
import javax.mail.internet.MimeMessage
import uk.ac.warwick.util.mail.WarwickMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import javax.mail.internet.MimeUtility

trait UnicodeEmails {

	def createMessage(sender: WarwickMailSender)(fn: => (MimeMessageHelper => Unit)) = prepareMessage(sender.createMimeMessage)(fn)

	def prepareMessage(message: MimeMessage)(fn: => (MimeMessageHelper => Unit)) = {
		val preparator = new FunctionalMimeMessagePreparator({ message =>
			val helper = new MimeMessageHelper(message, false, "UTF-8")
			fn(helper)
		})

		preparator.prepare(message)

		message
	}

	def encodeSubject(subject: String) = MimeUtility.encodeText(subject, "UTF-8", null)

}

class FunctionalMimeMessagePreparator(fn: => (MimeMessage => Unit)) extends MimeMessagePreparator {
	override def prepare(message: MimeMessage) = fn(message)
}