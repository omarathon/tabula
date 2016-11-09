package uk.ac.warwick.tabula.notifications

import java.util.concurrent.{ExecutionException, TimeUnit, TimeoutException}
import javax.mail.internet.MimeMessage

import org.hibernate.ObjectNotFoundException
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.data.model.{ActionRequiredNotification, HasNotificationAttachment, FreemarkerModel, Notification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{Logging, UnicodeEmails}
import uk.ac.warwick.tabula.services.{NotificationService, RecipientNotificationListener}
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.tabula.{CurrentUser, RequestInfo}
import uk.ac.warwick.util.mail.WarwickMailSender

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
	val replyWarning = "\n\nThis email was sent from an automated system and replies to it will not reach a real person."

	def link(n: Notification[_,_]) = if (n.isInstanceOf[ActionRequiredNotification]) {
		s"\n\nYou need to ${n.urlTitle}. Please visit $topLevelUrl${n.url}"
	} else {
		s"\n\nTo ${n.urlTitle}, please visit $topLevelUrl${n.url}"
	}

	// add an isEmail property for the model for emails
	def render(model: FreemarkerModel) = {
		textRenderer.renderTemplate(model.template, model.model + ("isEmail" -> true))
	}

	private def generateMessage(recipientInfo: RecipientNotificationInfo): Option[MimeMessage] = {
		try {
			val notification = recipientInfo.notification
			val recipient = recipientInfo.recipient

			Some(createMessage(mailSender) { message =>
				message.setFrom(fromAddress)
				message.setReplyTo(replyAddress)
				message.setTo(recipient.getEmail)
				message.setSubject(notification.title)

				val content: String = {
					// Access to restricted properties requires user inside RequestInfo
					val currentUser = new CurrentUser(recipient, recipient)
					val info = new RequestInfo(
						user = currentUser,
						requestedUri = null,
						requestParameters = Map()
					)
					RequestInfo.use(info) {
						render(notification.content)
					}
				}

				val body = new StringBuilder("")
				body.append(mailHeader.format(recipient.getFirstName))
				body.append(content)
				body.append(link(notification))
				body.append(mailFooter)
				body.append(replyWarning)
				message.setText(body.toString())

				// TODO I'm sure this can be pattern matched. but I can't get around the type checking
				if (notification.isInstanceOf[HasNotificationAttachment])
					notification.asInstanceOf[HasNotificationAttachment].generateAttachments(message)
			})
		} catch {
			// referenced entity probably missing, oh well.
			case e: ObjectNotFoundException => None
		}
	}

	def listen(recipientInfo: RecipientNotificationInfo) = {
		if (!recipientInfo.emailSent) {
			def cancelSendingEmail() {
				// TODO This is incorrect, really - we're not sending the email, we're cancelling the sending of the email
				recipientInfo.emailSent = true
				service.save(recipientInfo)
			}

			if (recipientInfo.dismissed) {
				logger.info(s"Not sending email for Notification as it is dismissed for $recipientInfo")
				cancelSendingEmail()
			} else if (recipientInfo.notification.priority < Notification.PriorityEmailThreshold) {
				logger.info(s"Not sending email as notification priority ${recipientInfo.notification.priority} below threshold")
				cancelSendingEmail()
			} else if (!recipientInfo.recipient.isFoundUser) {
				logger.error(s"Couldn't send email for Notification because usercode didn't match a user: $recipientInfo")
				cancelSendingEmail()
			} else if (recipientInfo.recipient.getEmail.isEmptyOrWhitespace) {
				logger.warn(s"Couldn't send email for Notification because recipient has no email address: $recipientInfo")
				cancelSendingEmail()
			} else if(recipientInfo.recipient.isLoginDisabled) {
				logger.warn(s"Couldn't send email for Notification because recipients login is disabed: $recipientInfo")
				cancelSendingEmail()
			} else {
				generateMessage(recipientInfo) match {
					case Some(message) =>
						val future = mailSender.send(message)
						try {
							val successful = future.get(30, TimeUnit.SECONDS)

							if (successful) {
								recipientInfo.emailSent = true
								service.save(recipientInfo)
							}
						} catch {
							case e: TimeoutException =>
								logger.info(s"Timeout waiting for message $message to be sent; cancelling to try again later", e)
								future.cancel(true)
							case e@(_: ExecutionException | _: InterruptedException) =>
								logger.warn("Could not send email ${message}, will try later", e)
						}
					case None =>
						logger.warn(s"Couldn't send email for Notification because object no longer exists: $recipientInfo")

						cancelSendingEmail()
				}
			}
		}
	}

}