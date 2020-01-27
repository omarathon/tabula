package uk.ac.warwick.tabula.notifications

import java.util.concurrent.{ExecutionException, TimeUnit, TimeoutException}

import javax.mail.internet.MimeMessage
import org.hibernate.ObjectNotFoundException
import org.joda.time.DateTime
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{Logging, UnicodeEmails}
import uk.ac.warwick.tabula.services.{BatchingRecipientNotificationListener, NotificationService}
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.tabula.{CurrentUser, RequestInfo}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.mail.WarwickMailSender

@Component
class EmailNotificationListener extends BatchingRecipientNotificationListener
  with UnicodeEmails
  with AutowiredTextRendererComponent
  with Logging {

  var topLevelUrl: String = Wire.property("${toplevel.url}")

  var mailSender: WarwickMailSender = Wire[WarwickMailSender]("studentMailSender")
  var service: NotificationService = Wire[NotificationService]

  // email constants
  var replyAddress: String = Wire.property("${mail.noreply.to}")
  var fromAddress: String = Wire.property("${mail.admin.to}")

  // add an isEmail property for the model for emails
  def render(model: FreemarkerModel): String = {
    textRenderer.renderTemplate(model.template, model.model + ("isEmail" -> true))
  }

  private def generateMessage(
    recipient: User,
    notificationTitle: => String,
    notificationBody: => FreemarkerModel,
    notificationUrl: => String,
    notificationUrlTitle: => String,
    notificationPriority: => NotificationPriority,
    actionRequired: => Boolean
  )(fn: MimeMessageHelper => Unit): MimeMessage =
    createMessage(mailSender, multipart = true) { message =>
      message.setFrom(fromAddress)
      message.setReplyTo(replyAddress)
      message.setTo(recipient.getEmail)
      message.setSubject(notificationTitle)

      val content: String = {
        // Access to restricted properties requires user inside RequestInfo
        val currentUser = new CurrentUser(recipient, recipient)
        val info = new RequestInfo(
          user = currentUser,
          requestedUri = null,
          requestParameters = Map()
        )
        RequestInfo.use(info) {
          render(notificationBody)
        }
      }

      val url = notificationUrl match {
        case absolute if absolute.startsWith("https://") => absolute
        case relative => s"$topLevelUrl$relative"
      }

      val plainText = textRenderer.renderTemplate("/WEB-INF/freemarker/emails/layout_plain.ftl", Map(
        "content" -> content,
        "recipient" -> recipient,
        "actionRequired" -> actionRequired,
        "url" -> url,
        "urlTitle" -> notificationUrlTitle
      ))

      val htmlText = textRenderer.renderTemplate("/WEB-INF/freemarker/emails/layout_html.ftlh", Map(
        "content" -> FormattedHtml(content),
        "preHeader" -> content.linesIterator.filterNot(_.isEmpty).nextOption(),
        "recipient" -> recipient,
        "actionRequired" -> actionRequired,
        "url" -> url,
        "urlTitle" -> notificationUrlTitle,
        "title" -> notificationTitle,
        "priority" -> notificationPriority.toNumericalValue
      ))

      message.setText(plainText, htmlText)
    }

  private def generateMessage(recipientInfo: RecipientNotificationInfo): Option[MimeMessage] =
    try {
      val notification = recipientInfo.notification
      val recipient = recipientInfo.recipient

      generateMessage(recipient, notification)
    } catch {
      // referenced entity probably missing, oh well.
      case t: ObjectNotFoundException =>
        logger.warn(s"Couldn't send email for Notification because object no longer exists: $recipientInfo", t)
        None
    }

  private def generateMessage(recipient: User, notification: Notification[_, _]): Option[MimeMessage] =
    try {
      Some(
        generateMessage(
          recipient = recipient,
          notificationTitle = notification.titleFor(recipient),
          notificationBody = notification.content,
          notificationUrl = notification.urlFor(recipient),
          notificationUrlTitle = notification.urlTitle,
          notificationPriority = notification.priority,
          actionRequired = notification.isInstanceOf[ActionRequiredNotification]
        ) { message =>
          notification match {
            case n: HasNotificationAttachment => n.generateAttachments(message)
            case _ => // do nothing
          }
        }
      )
    } catch {
      // referenced entity probably missing, oh well.
      case t: ObjectNotFoundException =>
        logger.warn(s"Couldn't send email for Notification because object no longer exists: $notification", t)
        None
    }

  private def generateMessagesForBatch(batch: Seq[RecipientNotificationInfo]): Seq[MimeMessage] =
    try {
      require(batch.size > 1, "The batch must include 2 or more notifications")

      val recipient = batch.head.recipient

      require(batch.forall { recipientInfo =>
        recipientInfo.recipient == recipient && recipientInfo.notification.notificationType == batch.head.notification.notificationType
      }, "All notifications in the batch must have the same recipient and be of the same type")

      require(batch.map(_.notification).forall(_.isInstanceOf[BaseBatchedNotification[_ >: Null <: ToEntityReference, _, _]]), "Notification must have the BatchedNotification trait")

      def cast(notification: Notification[_, _]): Notification[_, _] with BaseBatchedNotification[_ >: Null <: ToEntityReference, _, _] =
        notification.asInstanceOf[Notification[_, _] with BaseBatchedNotification[_ >: Null <: ToEntityReference, _, _]]

      val notifications = batch.map(rni => cast(rni.notification))
      val handler = notifications.head.batchedNotificationHandler.asInstanceOf[BatchedNotificationHandler[_ <: Notification[_, _]]]

      handler.groupBatch(notifications).flatMap {
        case Nil => None
        case Seq(notification) => generateMessage(recipient, notification)
        case group => generateMessageForBatch(recipient, group.map(cast), handler)
      }
    } catch {
      // referenced entity probably missing, oh well.
      case t: ObjectNotFoundException =>
        logger.warn(s"Couldn't send email for Notification because object no longer exists: $batch", t)
        Nil
    }

  private def generateMessageForBatch(recipient: User, notifications: Seq[Notification[_, _] with BaseBatchedNotification[_ >: Null <: ToEntityReference, _, _]], handler: BatchedNotificationHandler[_]): Option[MimeMessage] =
    try {
      require(notifications.size > 1, "The batch must include 2 or more notifications")

      Some(
        generateMessage(
          recipient = recipient,
          notificationTitle = handler.titleForBatch(notifications, recipient),
          notificationBody = handler.contentForBatch(notifications),
          notificationUrl = handler.urlForBatch(notifications, recipient),
          notificationUrlTitle = handler.urlTitleForBatch(notifications),
          notificationPriority = notifications.map(_.priority).max,
          actionRequired = notifications.exists(_.isInstanceOf[ActionRequiredNotification])
        ) { message =>
          notifications.foreach {
            case n: HasNotificationAttachment => n.generateAttachments(message)
            case _ =>
          }
        }
      )
    } catch {
      // referenced entity probably missing, oh well.
      case t: ObjectNotFoundException =>
        logger.warn(s"Couldn't send email for Notification because object no longer exists: $notifications", t)
        None
    }

  override def listen(recipientInfo: RecipientNotificationInfo): Unit =
    listenBatch(Seq(recipientInfo))

  override def listenBatch(allRecipients: Seq[RecipientNotificationInfo]): Unit = {
    val recipients = allRecipients.filterNot(_.emailSent)

    if (recipients.nonEmpty) {
      def cancelSendingEmail(recipientInfo: RecipientNotificationInfo): Unit = {
        // TODO This is incorrect, really - we're not sending the email, we're cancelling the sending of the email
        recipientInfo.emailSent = true
        service.save(recipientInfo)
      }

      val (validRecipients, invalidRecipients) = recipients.partition {
        case recipientInfo if recipientInfo.dismissed =>
          logger.info(s"Not sending email for Notification as it is dismissed for $recipientInfo")
          false

        case recipientInfo if recipientInfo.notification.priority < Notification.PriorityEmailThreshold =>
          logger.info(s"Not sending email as notification priority ${recipientInfo.notification.priority} below threshold: $recipientInfo")
          false

        case recipientInfo if !recipientInfo.recipient.isFoundUser =>
          logger.error(s"Couldn't send email for Notification because usercode didn't match a user: $recipientInfo")
          false

        case recipientInfo if recipientInfo.recipient.getEmail.isEmptyOrWhitespace =>
          logger.warn(s"Couldn't send email for Notification because recipient has no email address: $recipientInfo")
          false

        case recipientInfo if recipientInfo.recipient.isLoginDisabled =>
          logger.warn(s"Couldn't send email for Notification because recipients login is disabled: $recipientInfo")
          false

        case _ => true
      }

      invalidRecipients.foreach(cancelSendingEmail)

      val generatedMessage: Seq[MimeMessage] =
        if (validRecipients.isEmpty) Nil
        else if (validRecipients.size == 1) generateMessage(validRecipients.head).toSeq
        else generateMessagesForBatch(validRecipients)

      generatedMessage match {
        case Nil =>
          validRecipients.foreach(cancelSendingEmail)
        case messages =>
          val futures = messages.map(mailSender.send)
          try {
            val successful = futures.forall(_.get(30, TimeUnit.SECONDS))
            if (successful) {
              validRecipients.foreach(_.emailSent = true)
            }
          } catch {
            case e: TimeoutException =>
              logger.info(s"Timeout waiting for message $messages to be sent; cancelling to try again later", e)
              futures.foreach(_.cancel(true))
            case e@(_: ExecutionException | _: InterruptedException) =>
              logger.warn(s"Could not send email $messages, will try later", e)
          } finally {
            /* TAB-4544 log the time at which we tried to send this notification and save
             *
             * * gives us more info for diagnostics
             * * allows us to see which mails were cancelled (time attempted = null but sent is true)
             * * stops us from trying to send the same notification again and again if sending fails
             *   (don't send ones that we tried to send in the last 5 mins or so)
             */
            validRecipients.foreach { recipientInfo =>
              recipientInfo.attemptedAt = DateTime.now
              service.save(recipientInfo)
            }
          }
      }
    }
  }

}

trait EmailNotificationListenerComponent {
  def emailNotificationListener: BatchingRecipientNotificationListener
}

trait AutowiringEmailNotificationListenerComponent extends EmailNotificationListenerComponent {
  var emailNotificationListener: BatchingRecipientNotificationListener = Wire[EmailNotificationListener]
}
