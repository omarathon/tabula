package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import org.hibernate.criterion.{Restrictions, Order}
import uk.ac.warwick.util.hibernate.{BatchResultsImpl, BatchResults}
import uk.ac.warwick.tabula.data.model.{ToEntityReference, Notification}
import uk.ac.warwick.tabula.helpers.FunctionConversions.asGoogleFunction
import org.joda.time.DateTime

trait NotificationDao {
	def save(notification: Notification[_,_])

	def update(notification: Notification[_,_])

	def getById(id: String): Option[Notification[_  >: Null <: ToEntityReference, _]]

	def recent(start: DateTime): Scrollable[Notification[_,_]]
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

	override def save(notification: Notification[_,_]) {
		/**
		 * PreSaveBehaviour usually doesn't happen until flush, but we need
		 * properties to be set before flush at the moment so that the existing
		 * emailer can use those properties, so we call it manually here.
		 *
		 * In future we hopefully can get rid of this as the emailer will
		 * be fetching saved notifications from the database. Otherwise there
		 * are other pre-flush Hibernate event types we could create listeners for.
		 */
		if (notification.isInstanceOf[PreSaveBehaviour]) {
			val isNew = notification.id == null
			notification.asInstanceOf[PreSaveBehaviour].preSave(isNew)
		}
		session.save(notification)
	}

	def update(notification: Notification[_,_]) {
		session.saveOrUpdate(notification)
	}

	def getById(id: String) = getById[Notification[_ >: Null <: ToEntityReference,_]](id)
}
