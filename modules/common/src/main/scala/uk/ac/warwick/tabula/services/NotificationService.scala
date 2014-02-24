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

case class ActivityStreamRequest(
		user: User,
		max: Int = 100,
		priority: Double = 0,
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

	val listeners = Wire.all[NotificationListener]
	var dao = Wire[NotificationDao]
	var index = Wire[NotificationIndexServiceImpl]

	def push(notification: Notification[_,_]){
		// TODO - In future pushing a notification will add it to a queue, aggregate similar notifications etc.
		logger.info("Notification pushed - " + notification)
		dao.save(notification)
		this.notify(notification) // for now we just hard call notify
	}

	def notify[A](notification: Notification[_,_]) {
		logger.info("Notify listeners - " + notification)
		for (l <- listeners) l.listen(notification)
	}

	def stream(req: ActivityStreamRequest): PagingSearchResultItems[Activity[Any]] = {
		val notifications = index.userStream(req)
		val activities = notifications.items.flatMap { notification =>
			try {
				val content = notification.content
				val message = renderTemplate(content.template, content.model)

				Some(new Activity[Any](
					title = notification.title,
					date = notification.created,
					priority = notification.priority.toNumericalValue,
					agent = notification.agent,
					verb = notification.verb,
					url = notification.url,
					message = message,
					entity = null
				))
			} catch {
				// referenced entity probably missing, oh well.
				case e: ObjectNotFoundException => None
			}
		}
		notifications.copy(items = activities)
	}

}

trait NotificationListener {
	def listen(n: Notification[_,_]): Unit
}

trait NotificationServiceComponent {
	def notificationService: NotificationService
}

trait AutowiringNotificationServiceComponent {
	var notificationService = Wire[NotificationService]
}