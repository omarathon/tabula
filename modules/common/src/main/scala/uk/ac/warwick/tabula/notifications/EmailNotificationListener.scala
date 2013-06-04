package uk.ac.warwick.tabula.notifications

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.helpers.StringUtils._

class EmailNotificationListener extends UnicodeEmails {

	var mailSender = Wire[WarwickMailSender]("studentMailSender")

	// email constants
	var replyAddress: String = Wire.property("${mail.noreply.to}")
	var fromAddress: String = Wire.property("${mail.exceptions.to}")
	val mailHeader = "Dear %s,\n\n"
	val mailFooter = "\n\nThank you,\nTabula"
	val replyWarning = "\n\nThis email was sent from an automated system, and replies to it will not reach a real person."

	def listen:(Notification[_] => Unit) = notification => {
		val validRecipients = notification.recipients.filter(_.getEmail.hasText)
		validRecipients.foreach { recipient =>
			val message = createMessage(mailSender){ message =>
				message.setFrom(fromAddress)
				message.setReplyTo(replyAddress)
				message.setTo(recipient.getEmail)
				message.setSubject(notification.title)

				val body = new StringBuilder()
				body ++= mailHeader.format(recipient.getFirstName)
				body ++= notification.content
				body ++= mailFooter
				body ++= replyWarning
				message.setText(body.toString())
			}
			mailSender.send(message)
		}
	}
}