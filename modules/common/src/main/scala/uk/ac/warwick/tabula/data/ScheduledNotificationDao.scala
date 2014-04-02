package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.model.{ScheduledNotification, ToEntityReference}
import org.springframework.stereotype.Repository
import org.hibernate.criterion.{Order, Restrictions}

trait ScheduledNotificationDao {

		def save(scheduledNotification: ScheduledNotification[_]): Unit

		def delete(scheduledNotification: ScheduledNotification[_]): Unit

		def getById(id: String): Option[ScheduledNotification[_  >: Null <: ToEntityReference]]

		def getScheduledNotifications(entity: Any): Seq[ScheduledNotification[_  >: Null <: ToEntityReference]]

}

@Repository
class ScheduledNotificationDaoImpl extends ScheduledNotificationDao with Daoisms {

	override def save(scheduledNotification: ScheduledNotification[_]) = session.saveOrUpdate(scheduledNotification)

	override def getById(id: String) = getById[ScheduledNotification[_ >: Null <: ToEntityReference]](id)

	override def getScheduledNotifications(entity: Any) = {
		session.newCriteria[ScheduledNotification[_  >: Null <: ToEntityReference]]
			.createAlias("target", "target")
			.add(Restrictions.eq("target.entity", entity))
			.add(Restrictions.ne("completed", true))
			.addOrder(Order.asc("scheduledDate"))
			.seq
	}

	override def delete(scheduledNotification: ScheduledNotification[_]) = session.delete(scheduledNotification)
}