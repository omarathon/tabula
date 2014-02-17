package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import org.hibernate.criterion.Order
import uk.ac.warwick.util.hibernate.{BatchResultsImpl, BatchResults}
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.helpers.FunctionConversions.asGoogleFunction

trait NotificationDao {
	def save(notification: Notification[_,_])

	def getById(id: String): Option[Notification[_,_]]
	def recent(start: Int, count: Int): BatchResults[Notification[_,_]]
}

@Repository
class NotificationDaoImpl extends NotificationDao with Daoisms {

	private def idFunction(notification: Notification[_,_]) = notification.id

	def recent(start: Int, count: Int) = {
		val scrollable = session.newCriteria[Notification[_,_]]
			.addOrder(Order.desc("created"))
			.scroll()
		new BatchResultsImpl[Notification[_,_]](scrollable, 100, idFunction _, session)
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

	def getById(id: String) = getById[Notification[_,_]](id)
}
