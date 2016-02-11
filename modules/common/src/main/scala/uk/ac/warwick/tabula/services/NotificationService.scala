package uk.ac.warwick.tabula.services

import org.hibernate.ObjectNotFoundException
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.NotificationDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging}
import uk.ac.warwick.tabula.services.elasticsearch.{IndexedNotification, NotificationIndexService, NotificationQueryService}
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.userlookup.User

import scala.reflect.ClassTag

case class ActivityStreamRequest(
	user: User,
	max: Int = 100,
	priority: Double = 0,
	includeDismissed: Boolean = false,
	types: Option[Set[String]] = None,
	lastUpdatedDate: Option[DateTime]
)

case class ActivityStream(
	items: Seq[Activity[Any]],
	lastUpdatedDate: Option[DateTime],
	totalHits: Long
)

@Service
class NotificationService extends Logging with FreemarkerTextRenderer {

	var dao = Wire[NotificationDao]
	var userLookup = Wire[UserLookupService]
	var queryService = Wire[NotificationQueryService]
	var indexService = Wire[NotificationIndexService]

	def getNotificationById(id: String) = dao.getById(id)

	def push(notification: Notification[_,_]){
		// TODO - In future pushing a notification will add it to a queue, aggregate similar notifications etc.
		logger.info("Notification pushed - " + notification)
		dao.save(notification)

		// Individual listeners are responsible for pulling notifications
	}

	// update the notifications and rebuild their entries index
	def update(notifications: Seq[Notification[_, _]], user: User) {
		notifications.foreach(dao.update)

		indexService.indexItems(notifications.map { n => IndexedNotification(n.asInstanceOf[Notification[_ >: Null <: ToEntityReference, _]], user) })
	}

	def stream(req: ActivityStreamRequest): ActivityStream = {
		val notifications = queryService.userStream(req)
		val activities = notifications.items.flatMap(toActivity)

		ActivityStream(
			items = activities,
			lastUpdatedDate = notifications.lastUpdatedDate,
			totalHits = notifications.totalHits
		)
	}

	def toActivity(notification: Notification[_, _]) : Option[Activity[Any]] = {
		try {
			val (message, priority) =
				notification match {
					case actionRequired: ActionRequiredNotification if actionRequired.completed =>
						val actionRequired = notification.asInstanceOf[ActionRequiredNotification]
						val message = "Completed by %s on %s".format(
							userLookup.getUserByUserId(actionRequired.completedBy) match {
								case FoundUser(u) => u.getFullName
								case _ => "Unknown user"
							},
							notification.dateTimeFormatter.print(actionRequired.completedOn)
						)
						val priority = NotificationPriority.Complete
						(message, priority)

					case _ =>
						val content = notification.content
						val message = renderTemplate(content.template, content.model)
						val priority = notification.priorityOrDefault
						(message, priority)
				}

			Some(new Activity[Any](
				id = notification.id,
				title = notification.title,
				date = notification.created,
				priority = priority.toNumericalValue,
				agent = notification.agent,
				verb = notification.verb,
				url = notification.url,
				urlTitle = notification.urlTitle,
				message = message,
				entity = null
			))
		} catch {
			// referenced entity probably missing, oh well.
			case e: ObjectNotFoundException => None
		}
	}

	def save(recipientInfo: RecipientNotificationInfo) = transactional() {
		dao.save(recipientInfo)
	}

	def findActionRequiredNotificationsByEntityAndType[A <: ActionRequiredNotification : ClassTag](entity: ToEntityReference): Seq[ActionRequiredNotification] = {
		dao.findActionRequiredNotificationsByEntityAndType[A](entity)
	}

}

trait NotificationListener {
	def listen(notification: Notification[_,_]): Unit
}

trait RecipientNotificationListener {
	def listen(recipient: RecipientNotificationInfo): Unit
}

trait NotificationServiceComponent {
	def notificationService: NotificationService
}

trait AutowiringNotificationServiceComponent extends NotificationServiceComponent {
	var notificationService = Wire[NotificationService]
}