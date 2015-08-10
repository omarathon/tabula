package uk.ac.warwick.tabula.services

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.NotificationDao
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.util.queue.conversion.ItemType
import scala.collection.JavaConverters._
import scala.beans.BeanProperty
import uk.ac.warwick.tabula.data.Transactions._

/**
 * A manager that will automatically send and process messages for writable index operations. It's really hard to do this
 * generally because of having to deflate and reinflate the objects over the ActiveMQ queue, so expect to have to write a
 * wrapper method for each operation.
 */
trait IndexManager {
	def indexNotificationRecipients(notifications: Seq[Notification[_,_]], user: User)
	def createIndexMessage(notifications: Seq[Notification[_,_]], user: User): Object
	def indexNotificationRecipients(message: IndexNotificationRecipientsMessage)
}

@Service
class IndexManagerImpl extends IndexManager {

	@Autowired @Qualifier("indexTopic") var queue: Queue = _
	@Autowired var notificationIndexService: NotificationIndexService = _
	@Autowired var notificationDao: NotificationDao = _
	@Autowired var userLookup: UserLookupService = _

	def indexNotificationRecipients(notifications: Seq[Notification[_,_]], user: User) {
		queue.send(createIndexMessage(notifications, user))
	}

	def createIndexMessage(notifications: Seq[Notification[_,_]], user: User): Object = {
		// Create a message that we can broadcast to the scheduling war
		new IndexNotificationRecipientsMessage(notifications, user)
	}

	def indexNotificationRecipients(message: IndexNotificationRecipientsMessage) = transactional(readOnly = true) {
		// Convert back into notifications and user
		val notifications = message.notificationIds.asScala.flatMap(notificationDao.getById).toSeq
		val user = userLookup.getUserByUserId(message.userId)

		val recipientNotifications = notifications.map(new RecipientNotification(_, user))
		notificationIndexService.indexItems(recipientNotifications)
	}

}

@ItemType("IndexRecipients")
@JsonAutoDetect
class IndexNotificationRecipientsMessage {

	@BeanProperty var notificationIds: JList[String] = JArrayList()
	@BeanProperty var userId: String = _

	def this(notifications: Seq[Notification[_, _]], user: User) {
		this()

		notificationIds.addAll(notifications.map { _.id }.asJavaCollection)
		userId = user.getUserId
	}

}
