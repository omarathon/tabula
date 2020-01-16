package uk.ac.warwick.tabula.notifications

import org.hibernate.ObjectNotFoundException
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.scheduling.{AutowiringSchedulerComponent, SchedulerComponent}
import uk.ac.warwick.tabula.services.{AutowiringNotificationServiceComponent, BatchingRecipientNotificationListener, NotificationServiceComponent}
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, TextRendererComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.mywarwick.MyWarwickService
import uk.ac.warwick.util.mywarwick.model.request.{Activity, Tag}

import scala.jdk.CollectionConverters._
import scala.language.existentials

trait MyWarwickNotificationListener extends BatchingRecipientNotificationListener with Logging {
  self: TextRendererComponent
    with FeaturesComponent
    with MyWarwickServiceComponent
    with TopLevelUrlComponent
    with SchedulerComponent
    with NotificationServiceComponent =>

  private def generateActivity(
    recipient: User,
    notificationTitle: => String,
    notificationBody: => FreemarkerModel,
    notificationUrl: => String,
    notificationType: => String,
    entities: => Seq[PermissionsTarget]
  ): Activity = {
    def withParents(target: PermissionsTarget): LazyList[PermissionsTarget] = target #:: target.permissionsParents.flatMap(withParents)

    val tags = entities.flatMap(withParents).distinct.map { target =>
      val tag = new Tag()
      tag.setName(target.urlCategory)
      tag.setValue(target.urlSlug)
      tag.setDisplay_value(target.humanReadableId)
      tag
    }

    val notificationText: String = {
      // Access to restricted properties requires user inside RequestInfo
      val currentUser = new CurrentUser(recipient, recipient)
      val info = new RequestInfo(
        user = currentUser,
        requestedUri = null,
        requestParameters = Map()
      )
      RequestInfo.use(info) {
        textRenderer.renderTemplate(notificationBody.template, notificationBody.model)
      }
    }

    val activity = new Activity(
      Set(recipient.getUserId).asJava,
      notificationTitle.replaceAll("(\\.)+$", ""),
      notificationUrl match {
        case absolute if absolute.startsWith("https://") => absolute
        case relative => s"$toplevelUrl$relative"
      },
      notificationText,
      notificationType
    )

    activity.setTags(tags.toSet.asJava)
    activity
  }

  private def allEntities(notification: Notification[_ >: Null <: ToEntityReference, _]): Seq[PermissionsTarget] = {
    val entities = notification match {
      case targetNotification: NotificationWithTarget[_, _] => targetNotification.items.asScala :+ targetNotification.target
      case _ => notification.items.asScala
    }

    entities.filter(_ != null).map(_.entity).collect { case pt: PermissionsTarget => pt }.toSeq
  }

  private def generateActivity(recipientInfo: RecipientNotificationInfo): Option[Activity] =
    try {
      val notification = recipientInfo.notification.asInstanceOf[Notification[_ >: Null <: ToEntityReference, _]]
      val recipient = recipientInfo.recipient

      Some(
        generateActivity(
          recipient = recipient,
          notificationTitle = notification.titleFor(recipient),
          notificationBody = notification.content,
          notificationUrl = notification.urlFor(recipient),
          notificationType = notification.notificationType,
          entities = allEntities(notification)
        )
      )
    } catch {
      // referenced entity probably missing, oh well.
      case _: ObjectNotFoundException => None
    }

  private def generateActivityForBatch(batch: Seq[RecipientNotificationInfo]): Option[Activity] =
    try {
      require(batch.size > 1, "The batch must include 2 or more notifications")

      val recipient = batch.head.recipient

      require(batch.forall { recipientInfo =>
        recipientInfo.recipient == recipient && recipientInfo.notification.notificationType == batch.head.notification.notificationType
      }, "All notifications in the batch must have the same recipient and be of the same type")

      require(batch.map(_.notification).forall(_.isInstanceOf[BatchedNotification[_]]), "Notification must have the BatchedNotification trait")

      val notifications = batch.map(_.notification.asInstanceOf[Notification[_ >: Null <: ToEntityReference, _] with BatchedNotification[_]])
      val referenceNotification = notifications.head

      Some(
        generateActivity(
          recipient = recipient,
          notificationTitle = referenceNotification.titleForBatch(notifications, recipient),
          notificationBody = referenceNotification.contentForBatch(notifications),
          notificationUrl = referenceNotification.urlForBatch(notifications, recipient),
          notificationType = referenceNotification.notificationType,
          entities = notifications.flatMap(allEntities)
        )
      )
    } catch {
      // referenced entity probably missing, oh well.
      case t: ObjectNotFoundException =>
        logger.warn(s"Couldn't send email for Notification because object no longer exists: $batch", t)
        None
    }

  def postActivity(allRecipients: Seq[RecipientNotificationInfo]): Unit = {
    val recipients = allRecipients.filterNot(_.myWarwickActivitySent)

    if (recipients.nonEmpty) {
      def cancelSendingActivity(recipientInfo: RecipientNotificationInfo): Unit = {
        // TODO This is incorrect, really - we're not sending the activity, we're cancelling the sending of the activity
        recipientInfo.myWarwickActivitySent = true
        notificationService.save(recipientInfo)
      }

      val (validRecipients, invalidRecipients) = recipients.partition {
        case recipientInfo if recipientInfo.dismissed =>
          logger.info(s"Not sending My Warwick activity for Notification as it is dismissed for $recipientInfo")
          false

        case recipientInfo if !recipientInfo.recipient.isFoundUser =>
          logger.error(s"Couldn't send My Warwick activity for Notification because usercode didn't match a user: $recipientInfo")
          false

        case recipientInfo if recipientInfo.recipient.isLoginDisabled =>
          logger.warn(s"Couldn't send My Warwick activity for Notification because recipients login is disabled: $recipientInfo")
          false

        case _ => true
      }

      invalidRecipients.foreach(cancelSendingActivity)

      val generatedActivity: Option[Activity] =
        if (validRecipients.isEmpty) None
        else if (validRecipients.size == 1) generateActivity(validRecipients.head)
        else generateActivityForBatch(validRecipients)

      generatedActivity.foreach { activity =>
        if (validRecipients.head.notification.isInstanceOf[MyWarwickNotification]) {
          logger.info(s"Sending MyWarwick notification - ${activity.getTitle} to ${activity.getRecipients.getUsers.asScala.mkString(", ")}")
          myWarwickService.queueNotification(activity, scheduler)
        } else {
          logger.info(s"Sending MyWarwick activity - ${activity.getTitle} to ${activity.getRecipients.getUsers.asScala.mkString(", ")}")
          myWarwickService.queueActivity(activity, scheduler)
        }
      }

      recipients.foreach { recipient =>
        recipient.myWarwickActivitySent = true
        notificationService.save(recipient)
      }
    } else Seq()
  }

  override def listen(recipient: RecipientNotificationInfo): Unit =
    if (features.myWarwickNotificationListener) {
      postActivity(Seq(recipient))
    }

  override def listenBatch(recipients: Seq[RecipientNotificationInfo]): Unit =
    if (features.myWarwickNotificationListener) {
      postActivity(recipients)
    }

}

@Component("myWarwickNotificationListener")
class AutowiringMyWarwickNotificationListener
  extends MyWarwickNotificationListener
    with AutowiredTextRendererComponent
    with AutowiringFeaturesComponent
    with AutowiringMyWarwickServiceComponent
    with AutowiringTopLevelUrlComponent
    with AutowiringSchedulerComponent
    with AutowiringNotificationServiceComponent

trait MyWarwickNotificationListenerComponent {
  def myWarwickNotificationListener: BatchingRecipientNotificationListener
}

trait AutowiringMyWarwickNotificationListenerComponent extends MyWarwickNotificationListenerComponent {
  var myWarwickNotificationListener: BatchingRecipientNotificationListener = Wire[MyWarwickNotificationListener]
}

trait MyWarwickServiceComponent {
  def myWarwickService: MyWarwickService
}

trait AutowiringMyWarwickServiceComponent extends MyWarwickServiceComponent {
  var myWarwickService: MyWarwickService = Wire[MyWarwickService]
}
