package uk.ac.warwick.tabula.notifications

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.tabula.helpers.{Logging, UnicodeEmails}
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, Notification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{RecipientNotificationListener, NotificationService, NotificationListener}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import java.util.concurrent.{ExecutionException, TimeoutException, TimeUnit}

@Component
class EmailNotificationListener extends RecipientNotificationListener with UnicodeEmails with AutowiredTextRendererComponent with Logging {

	var topLevelUrl: String = Wire.property("${toplevel.url}")

	var mailSender = Wire[WarwickMailSender]("studentMailSender")
	var service = Wire[NotificationService]

	// email constants
	var replyAddress: String = Wire.property("${mail.noreply.to}")
	var fromAddress: String = Wire.property("${mail.admin.to}")
	val mailHeader = "Dear %s,\n\n"
	val mailFooter = "\n\nThank you,\nTabula"
	val replyWarning = "\n\nThis email was sent from an automated system, and replies to it will not reach a real person."

	def link(n: Notification[_,_]) = if(n.actionRequired) {
		s"\n\nYou need to ${n.urlTitle}. Please visit ${topLevelUrl}${n.url}."
	} else {
		s"\n\nTo ${n.urlTitle}, please visit ${topLevelUrl}${n.url}."
	}

	// add an isEmail property for the model for emails
	def render(model: FreemarkerModel) = textRenderer.renderTemplate(model.template, model.model + ("isEmail" -> true))

	def listen(recipientInfo: RecipientNotificationInfo) = {
		if (!recipientInfo.emailSent && recipientInfo.recipient.getEmail.hasText) {
			val notification = recipientInfo.notification
			val recipient = recipientInfo.recipient

			val message = createMessage(mailSender) { message =>
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

			val future = mailSender.send(message)
			try {
				val successful = future.get(30, TimeUnit.SECONDS)

				if (successful) {
					recipientInfo.emailSent = true
					service.save(recipientInfo)
				}
			} catch {
				case e: TimeoutException => {
					logger.info(s"Timeout waiting for message ${message} to be sent; cancelling to try again later", e)
					future.cancel(true)
				}
				case e @ (_: ExecutionException | _: InterruptedException) => {
					logger.warn("Could not send email ${message}, will try later", e)
				}
			}
		}
	}

}