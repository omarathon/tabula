package uk.ac.warwick.tabula.notifications

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, Notification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.NotificationListener
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent

@Component
class EmailNotificationListener extends NotificationListener with UnicodeEmails with AutowiredTextRendererComponent {

	var topLevelUrl: String = Wire.property("${toplevel.url}")

	var mailSender = Wire[WarwickMailSender]("studentMailSender")

	// email constants
	var replyAddress: String = Wire.property("${mail.noreply.to}")
	var fromAddress: String = Wire.property("${mail.exceptions.to}")
	val mailHeader = "Dear %s,\n\n"
	val mailFooter = "\n\nThank you,\nTabula"
	val replyWarning = "\n\nThis email was sent from an automated system, and replies to it will not reach a real person."

	def link(n: Notification[_,_]) = s"\n\nTo ${n.urlTitle}, please visit ${topLevelUrl}${n.url}."

	// add an isEmail property for the model for emails
	def render(model: FreemarkerModel) = textRenderer.renderTemplate(model.template, model.model + ("isEmail" -> true))

	def listen(notification: Notification[_,_]) {
		val validRecipients = notification.recipients.filter(_.getEmail.hasText)
		validRecipients.foreach { recipient =>
			val message = createMessage(mailSender){ message =>
				message.setFrom(fromAddress)
				message.setReplyTo(replyAddress)
				message.setTo(recipient.getEmail)
				message.setSubject(notification.title)

				val body = new StringBuilder("")
				body.append(mailHeader.format(recipient.getFirstName))
				body.append(render(notification.content))
				body.append(link(notification))
				body.append(mailFooter)
				body.append(replyWarning)
				message.setText(body.toString())
			}
			mailSender.send(message)
		}
	}

}