package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Restrictions._
import org.hibernate.criterion.Projections._
import org.hibernate.criterion.Order._
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.data.model.{ActionRequiredNotification, Notification, ToEntityReference}
import uk.ac.warwick.userlookup.User

import scala.reflect.ClassTag

object NotificationDao {
  // if a notification fails to send don't retry for this many minutes
  val RETRY_DELAY_MINUTES = 5
}

trait NotificationDao {
  def save(notification: Notification[_, _]): Unit

  def save(recipientInfo: RecipientNotificationInfo): Unit

  def update(notification: Notification[_, _]): Unit

  def getById(id: String): Option[Notification[_ >: Null <: ToEntityReference, _]]

  def findActionRequiredNotificationsByEntityAndType[A <: ActionRequiredNotification : ClassTag](entity: ToEntityReference): Seq[ActionRequiredNotification]

  def recent(start: DateTime): Scrollable[Notification[_ >: Null <: ToEntityReference, _]]

  def unemailedRecipientCount: Number

  def unemailedRecipientIds(count: Int): Seq[String]

  /**
   * @return tuple of recipient, notification type, earliest creation date;
   *         sorted by earliest creation date
   */
  def unemailedRecipientsByNotificationType(count: Int): Seq[(User, String, DateTime)]

  def unemailedNotificationsFor(recipient: User, notificationType: String, retryBackoff: Boolean): Seq[RecipientNotificationInfo]

  def oldestUnemailedRecipient: Option[RecipientNotificationInfo]

  def recentEmailedRecipient: Option[RecipientNotificationInfo]

  def recentRecipients(start: Int, count: Int): Seq[RecipientNotificationInfo]

  def unprocessedNotificationCount: Number

  def unprocessedNotifications: Scrollable[Notification[_ >: Null <: ToEntityReference, _]]
}

@Repository
class NotificationDaoImpl extends NotificationDao with Daoisms {

  /** A Scrollable of all notifications since this date, sorted date ascending.
    */
  def recent(start: DateTime): Scrollable[Notification[_ >: Null <: ToEntityReference, _]] = {
    val scrollable = session.newCriteria[Notification[_ >: Null <: ToEntityReference, _]]
      .add(ge("created", start))
      .addOrder(asc("created"))
      .scroll()
    Scrollable(scrollable, session)
  }

  private def unemailedRecipientCriteria(retryBackoff: Boolean): ScalaCriteria[RecipientNotificationInfo] = {
    val c =
      session.newCriteria[RecipientNotificationInfo]
        .createAlias("notification", "notification")
        .add(is("emailSent", false))
        .add(is("dismissed", false))

    if (retryBackoff)
      c.add(or(
        isNull("attemptedAt"),
        lt("attemptedAt", DateTime.now.minusMinutes(NotificationDao.RETRY_DELAY_MINUTES))
      ))

    c
  }

  def unemailedRecipientCount: Number =
    unemailedRecipientCriteria(retryBackoff = false).count

  def unemailedRecipientIds(count: Int): Seq[String] =
    unemailedRecipientCriteria(retryBackoff = true)
      .addOrder(asc("notification.created"))
      .setMaxResults(count)
      .project[String](id())
      .seq

  def unemailedRecipientsByNotificationType(count: Int): Seq[(User, String, DateTime)] =
    unemailedRecipientCriteria(retryBackoff = true)
      .project[Array[Object]](
        projectionList()
          .add(groupProperty("recipient"))
          .add(groupProperty("notification._notificationType"))
          .add(alias(min("notification.created"), "created"))
      )
      .addOrder(asc("created"))
      .setMaxResults(count)
      .seq.collect {
        case Array(recipient: User, notificationType: String, earliestCreated: DateTime) =>
          (recipient, notificationType, earliestCreated)
      }

  def unemailedNotificationsFor(recipient: User, notificationType: String, retryBackoff: Boolean): Seq[RecipientNotificationInfo] =
    unemailedRecipientCriteria(retryBackoff)
      .add(is("recipient", recipient))
      .add(is("notification._notificationType", notificationType))
      .addOrder(asc("created"))
      .seq

  def oldestUnemailedRecipient: Option[RecipientNotificationInfo] =
    unemailedRecipientCriteria(retryBackoff = false)
      .addOrder(asc("notification.created"))
      .setMaxResults(1)
      .seq.headOption

  def recentEmailedRecipient: Option[RecipientNotificationInfo] = {
    session.newCriteria[RecipientNotificationInfo]
      .createAlias("notification", "notification")
      .add(is("emailSent", true))
      .add(is("dismissed", false))
      .add(gt("attemptedAt", DateTime.now.minusDays(1)))
      .addOrder(desc("notification.created"))
      .setMaxResults(1)
      .seq.headOption
  }

  def recentRecipients(start: Int, count: Int): Seq[RecipientNotificationInfo] =
    session.newCriteria[RecipientNotificationInfo]
      .createAlias("notification", "notification")
      .setFetchMode("notification", FetchMode.JOIN)
      .add(or(is("emailSent", true), is("dismissed", false)))
      .addOrder(asc("emailSent"))
      .addOrder(desc("notification.created"))
      .setFirstResult(start)
      .setMaxResults(count)
      .seq

  def save(notification: Notification[_, _]): Unit = {
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

  def save(recipientInfo: RecipientNotificationInfo): Unit = {
    session.saveOrUpdate(recipientInfo)
  }

  def update(notification: Notification[_, _]): Unit = {
    session.saveOrUpdate(notification)
  }

  def getById(id: String): Option[Notification[_ >: Null <: ToEntityReference, _]] = getById[Notification[_ >: Null <: ToEntityReference, _]](id)

  def findActionRequiredNotificationsByEntityAndType[A <: ActionRequiredNotification : ClassTag](entity: ToEntityReference): Seq[ActionRequiredNotification] = {
    val targetEntity = entity match {
      case ref: ToEntityReference => ref.toEntityReference.entity
      case _ => entity
    }
    session.newCriteria[A]
      .createAlias("items", "items")
      .add(is("items.entity", targetEntity))
      .seq
  }

  private def unprocessedNotificationCriteria =
    session.newCriteria[Notification[_ >: Null <: ToEntityReference, _]]
      .add(is("_listenersProcessed", false))

  def unprocessedNotificationCount: Number =
    unprocessedNotificationCriteria.count

  def unprocessedNotifications: Scrollable[Notification[_ >: Null <: ToEntityReference, _]] = {
    val scrollable = unprocessedNotificationCriteria
      .addOrder(asc("created"))
      .scroll()
    Scrollable(scrollable, session)
  }
}

trait NotificationDaoComponent {
  def notificationDao: NotificationDao
}

trait AutowiringNotificationDaoComponent extends NotificationDaoComponent {
  var notificationDao: NotificationDao = Wire[NotificationDao]
}
