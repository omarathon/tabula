package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import org.hibernate.criterion.{Restrictions, Order}
import uk.ac.warwick.util.hibernate.{BatchResultsImpl, BatchResults}
import uk.ac.warwick.tabula.data.model.{ToEntityReference, Notification}
import uk.ac.warwick.tabula.helpers.FunctionConversions.asGoogleFunction
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import org.hibernate.FetchMode

trait NotificationDao {
	def save(notification: Notification[_,_])
	def save(recipientInfo: RecipientNotificationInfo)

	def update(notification: Notification[_,_])

	def getById(id: String): Option[Notification[_  >: Null <: ToEntityReference, _]]

	def recent(start: DateTime): Scrollable[Notification[_,_]]
	def unemailedRecipientCount: Number
	def unemailedRecipients: Scrollable[RecipientNotificationInfo]
	def recentRecipients(start: Int, count: Int): Seq[RecipientNotificationInfo]
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
		Scrollable(scrollable, session)
	}

	private def unemailedRecipientCriteria =
		session.newCriteria[RecipientNotificationInfo]
			.createAlias("notification", "notification")
			.add(is("emailSent", false))

	def unemailedRecipientCount =
		unemailedRecipientCriteria.count

	def unemailedRecipients: Scrollable[RecipientNotificationInfo] = {
		val scrollable = unemailedRecipientCriteria
			.addOrder(Order.asc("notification.created"))
			.scroll()
		Scrollable(scrollable, session)
	}

	def recentRecipients(start: Int, count: Int): Seq[RecipientNotificationInfo] =
		session.newCriteria[RecipientNotificationInfo]
			.createAlias("notification", "notification")
			.setFetchMode("notification", FetchMode.JOIN)
			.addOrder(Order.asc("emailSent"))
			.addOrder(Order.desc("notification.created"))
			.setFirstResult(start)
			.setMaxResults(count)
			.seq

	def save(notification: Notification[_,_]) {
		/**
		 * FIXME This should no longer be required but submitting assignments
		 * doesn't work without it.
		 *
		 * PreSaveBehaviour usually doesn't happen until flush, but we need
		 * properties to be set before flush to avoid ConcurrentModificationExceptions.
		 *
		 * There are other pre-flush Hibernate event types we could create listeners for.
		 */
		val isNew = notification.id == null
		notification.preSave(isNew)

		session.save(notification)
		session.flush() // TAB-2381
	}

	def save(recipientInfo: RecipientNotificationInfo) {
		session.saveOrUpdate(recipientInfo)
	}

	def update(notification: Notification[_,_]) {
		session.saveOrUpdate(notification)
	}

	def getById(id: String) = getById[Notification[_ >: Null <: ToEntityReference,_]](id)
}
