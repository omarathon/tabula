package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.data.model.{ScheduledNotification, ToEntityReference}
import org.springframework.stereotype.Repository
import org.hibernate.criterion.{Order, Restrictions}
import org.joda.time.DateTime

trait ScheduledNotificationDao {
	def save(scheduledNotification: ScheduledNotification[_]): Unit
	def delete(scheduledNotification: ScheduledNotification[_]): Unit

	def getById(id: String): Option[ScheduledNotification[_  >: Null <: ToEntityReference]]

	def notificationsToComplete: Scrollable[ScheduledNotification[_  >: Null <: ToEntityReference]]

	def getScheduledNotifications[A >: Null <: ToEntityReference](entity: A): Seq[ScheduledNotification[A]]
}

@Repository
class ScheduledNotificationDaoImpl extends ScheduledNotificationDao with Daoisms {

	override def save(scheduledNotification: ScheduledNotification[_]): Unit = {
		session.saveOrUpdate(scheduledNotification)
	}

	override def getById(id: String): Option[ScheduledNotification[_ >: Null <: ToEntityReference]] = getById[ScheduledNotification[_ >: Null <: ToEntityReference]](id)

	override def getScheduledNotifications[A >: Null <: ToEntityReference](targetEntity: A): Seq[ScheduledNotification[A]] = {
		session.newCriteria[ScheduledNotification[A]]
			.createAlias("target", "target")
			.add(Restrictions.eq("target.entity", targetEntity))
			.add(Restrictions.ne("completed", true))
			.addOrder(Order.asc("scheduledDate"))
			.seq
	}

	override def delete(scheduledNotification: ScheduledNotification[_]): Unit = session.delete(scheduledNotification)

	override def notificationsToComplete: Scrollable[ScheduledNotification[_  >: Null <: ToEntityReference]] = {
		val scrollable =
			session.newCriteria[ScheduledNotification[_  >: Null <: ToEntityReference]]
				.add(Restrictions.ne("completed", true))
				.add(Restrictions.le("scheduledDate", DateTime.now))
				.addOrder(Order.asc("scheduledDate"))
				.scroll()
		Scrollable(scrollable, session)
	}
}