package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{Activity, Notification}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.NotificationDao
import uk.ac.warwick.userlookup.User
import org.apache.lucene.search.FieldDoc
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import org.hibernate.ObjectNotFoundException
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.data.Transactions._

case class ActivityStreamRequest(
		user: User,
		max: Int = 100,
		priority: Double = 0,
		includeDismissed: Boolean = false,
		types: Option[Set[String]] = None,
		pagination: Option[SearchPagination]
)

case class SearchPagination(lastDoc: Int, lastField: Long, token: Long) {
	def this(last: FieldDoc, token: Long) {
		this(last.doc, last.fields(0).asInstanceOf[Long], token)
	}
}

@Service
class NotificationService extends Logging with FreemarkerTextRenderer {

	var dao = Wire[NotificationDao]
	var index = Wire[NotificationIndexServiceImpl]

	def getNotificationById(id: String) = dao.getById(id)

	def push(notification: Notification[_,_]){
		// TODO - In future pushing a notification will add it to a queue, aggregate similar notifications etc.
		logger.info("Notification pushed - " + notification)
		dao.save(notification)
	}

	// update the notifications and rebuild their entries index
	def update(notifications: Seq[Notification[_,_]], user: User) {
		notifications.foreach(dao.update)
		val recipientNotifications = notifications.map(new RecipientNotification(_, user))
		index.indexItems(recipientNotifications)
	}

	def stream(req: ActivityStreamRequest): PagingSearchResultItems[Activity[Any]] = {
		val notifications = index.userStream(req)
		val activities = notifications.items.flatMap(toActivity)
		notifications.copy(items = activities)
	}

	def toActivity(notification: Notification[_,_]) : Option[Activity[Any]] = {
		try {
			val content = notification.content
			val message = renderTemplate(content.template, content.model)

			Some(new Activity[Any](
				id = notification.id,
				title = notification.title,
				date = notification.created,
				priority = notification.priorityOrDefault.toNumericalValue,
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