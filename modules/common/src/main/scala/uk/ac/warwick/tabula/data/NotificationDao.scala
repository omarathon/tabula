package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import org.hibernate.criterion.{Restrictions, Order}
import uk.ac.warwick.util.hibernate.{BatchResultsImpl, BatchResults}
import uk.ac.warwick.tabula.data.model.{ToEntityReference, Notification}
import uk.ac.warwick.tabula.helpers.FunctionConversions.asGoogleFunction
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo

trait NotificationDao {
	def save(notification: Notification[_,_])
	def save(recipientInfo: RecipientNotificationInfo)

	def update(notification: Notification[_,_])

	def getById(id: String): Option[Notification[_  >: Null <: ToEntityReference, _]]

	def recent(start: DateTime): Scrollable[Notification[_,_]]
	def unemailedRecipients: Scrollable[RecipientNotificationInfo]
}

@Repository
class NotificationDaoImpl extends NotificationDao with Daoisms {

	private def idFunction(notification: Notification[_,_]) = notification.id

	/** A Scrollable of all notifications since this date, sorted date ascending.
		*/
	def recent(start: DateTime): Scrollable[Notification[_,_]] = {
		val scrollable = session.newCriteria[Notification[_,_]]
			.add(Restrictions.ge("created", start))
			.addOrder(Order.asc("created"))
			.scroll()
		new Scrollable(scrollable, session)
	}

	def unemailedRecipients: Scrollable[RecipientNotificationInfo] = {
		val scrollable = session.newCriteria[RecipientNotificationInfo]
			.createAlias("notification", "notification")
			.add(is("emailSent", false))
			.addOrder(Order.asc("notification.created"))
			.scroll()
		new Scrollable(scrollable, session)
	}

	def save(notification: Notification[_,_]) {
		session.save(notification)
	}

	def save(recipientInfo: RecipientNotificationInfo) {
		session.saveOrUpdate(recipientInfo)
	}

	def update(notification: Notification[_,_]) {
		session.saveOrUpdate(notification)
	}

	def getById(id: String) = getById[Notification[_ >: Null <: ToEntityReference,_]](id)
}
