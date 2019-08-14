package uk.ac.warwick.tabula.notifications

import org.hibernate.ObjectNotFoundException
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.NotificationListener
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, TextRendererComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.mywarwick.MyWarwickService
import uk.ac.warwick.util.mywarwick.model.request.{Activity, Tag}

import scala.collection.JavaConverters._
import scala.language.existentials

trait MyWarwickNotificationListener extends NotificationListener with Logging {
  self: TextRendererComponent with FeaturesComponent with MyWarwickServiceComponent with TopLevelUrlComponent =>

  def toMyWarwickActivities(notification: Notification[_ >: Null <: ToEntityReference, _]): Seq[Activity] = try {
    val recipients = notification.recipientNotificationInfos.asScala
      .filterNot(_.dismissed) // Not if the user has dismissed the notification already
      .map(_.recipient)
      .filter(_.isFoundUser) // Only users found in SSO

    if (recipients.isEmpty) Seq()
    else {
      val allEntities = notification match {
        case targetNotification: NotificationWithTarget[_, _] => targetNotification.items.asScala :+ targetNotification.target
        case _ => notification.items.asScala
      }

      val permissionsTargets = allEntities.filter(_ != null).map(_.entity).collect { case pt: PermissionsTarget => pt }

      def withParents(target: PermissionsTarget): Stream[PermissionsTarget] = target #:: target.permissionsParents.flatMap(withParents)

      val tags = permissionsTargets.flatMap(withParents).distinct.map { target =>
        val tag = new Tag()
        tag.setName(target.urlCategory)
        tag.setValue(target.urlSlug)
        tag.setDisplay_value(target.humanReadableId)
        tag
      }

      def getRecipientActivity(recipient: User): Activity = {
        val notificationText: String = {
          // Access to restricted properties requires user inside RequestInfo
          val currentUser = new CurrentUser(recipient, recipient)
          val info = new RequestInfo(
            user = currentUser,
            requestedUri = null,
            requestParameters = Map()
          )
          RequestInfo.use(info) {
            textRenderer.renderTemplate(notification.content.template, notification.content.model)
          }
        }

        val activity = new Activity(
          Set(recipient.getUserId).asJava,
          notification.title,
          if (notification.url.toLowerCase.startsWith("https://")) {
            notification.url
          } else {
            toplevelUrl + notification.url
          },
          notificationText,
          notification.notificationType
        )

        activity.setTags(tags.toSet.asJava)

        activity
      }

    recipients.map(recipient => getRecipientActivity(recipient))

    }
  } catch {
    // referenced entity probably missing, oh well.
    case _: ObjectNotFoundException => Seq()
  }

  private def postActivity(notification: Notification[_ >: Null <: ToEntityReference, _]): Unit = {
    notification match {
      case a: MyWarwickNotification => toMyWarwickActivities(notification).map(n => {
        logger.info(s"Sending MyWarwick notification - ${n.getTitle} to ${n.getRecipients.getUsers.asScala.mkString(", ")}")
        myWarwickService.sendAsNotification(n)
      })
      case a => toMyWarwickActivities(notification).map(a => {
        logger.info(s"Sending MyWarwick activity - ${a.getTitle} to ${a.getRecipients.getUsers.asScala.mkString(", ")}")
        myWarwickService.sendAsActivity(a)
      })
    }
  }

  override def listen(notification: Notification[_ >: Null <: ToEntityReference, _]): Unit = {
    if (features.myWarwickNotificationListener) {
      postActivity(notification)
    }
  }

}

@Component
class AutowiringMyWarwickNotificationListener extends MyWarwickNotificationListener
  with AutowiredTextRendererComponent
  with AutowiringFeaturesComponent
  with AutowiringMyWarwickServiceComponent
  with AutowiringTopLevelUrlComponent

trait AutowiringMyWarwickServiceComponent extends MyWarwickServiceComponent {
  var myWarwickService: MyWarwickService = Wire[MyWarwickService]
}

trait MyWarwickServiceComponent {
  var myWarwickService: MyWarwickService
}