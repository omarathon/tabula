package uk.ac.warwick.tabula.notifications

import org.hibernate.ObjectNotFoundException
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.NotificationListener
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, TextRendererComponent}
import uk.ac.warwick.util.mywarwick.MyWarwickService
import uk.ac.warwick.util.mywarwick.model.request.{Activity, Tag}

import scala.collection.JavaConverters._
import scala.language.existentials

trait MyWarwickNotificationListener extends NotificationListener {
	self: TextRendererComponent with FeaturesComponent with MyWarwickServiceComponent with TopLevelUrlComponent =>

	private def toMyWarwickActivity(notification: Notification[_ >: Null <: ToEntityReference, _]): Option[Activity] = try {
		val recipients = notification.recipientNotificationInfos.asScala
			.filterNot(_.dismissed) // Not if the user has dismissed the notification already
			.map(_.recipient)
			.filter(_.isFoundUser) // Only users found in SSO
			.map(_.getUserId)

		if (recipients.isEmpty) None
		else {
			val allEntities = notification match {
				case targetNotification: NotificationWithTarget[_, _] => targetNotification.items.asScala :+ targetNotification.target
				case _ => notification.items.asScala
			}

			val permissionsTargets = allEntities.filter(_ != null).map {
				_.entity
			}.collect { case pt: PermissionsTarget => pt }

			def withParents(target: PermissionsTarget): Stream[PermissionsTarget] = target #:: target.permissionsParents.flatMap(withParents)

			val tags = permissionsTargets.flatMap(withParents).distinct.map { target =>
				val tag = new Tag()
				tag.setName(target.urlCategory)
				tag.setValue(target.urlSlug)
				tag.setDisplay_value(target.humanReadableId)
				tag
			}

			val activity = new Activity(
				recipients.toSet.asJava,
				notification.title,
				if (notification.url.toLowerCase.startsWith("https://")) {
					notification.url
				} else {
					toplevelUrl + notification.url
				},
				textRenderer.renderTemplate(notification.content.template, notification.content.model),
				notification.notificationType
			)

			activity.setTags(tags.toSet.asJava)

			Some(activity)
		}
	} catch {
		// referenced entity probably missing, oh well.
		case _: ObjectNotFoundException => None
	}

	private def postActivity(notification: Notification[_ >: Null <: ToEntityReference, _]): Unit = {
		notification match {
			case a: MyWarwickNotification => toMyWarwickActivity(notification).map(myWarwickService.sendAsNotification)
			case a => toMyWarwickActivity(notification).map(myWarwickService.sendAsActivity)
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