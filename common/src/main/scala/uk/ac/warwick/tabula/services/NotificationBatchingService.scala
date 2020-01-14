package uk.ac.warwick.tabula.services

import org.joda.time.{DateTime, Seconds}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.data.model.{BatchedNotification, Notification, ToEntityReference, UserSettings}
import uk.ac.warwick.tabula.data.{AutowiringNotificationDaoComponent, Daoisms, NotificationDaoComponent, SessionComponent}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.email
import uk.ac.warwick.tabula.helpers.{Logging, ReflectionHelper}
import uk.ac.warwick.tabula.notifications.{AutowiringEmailNotificationListenerComponent, AutowiringMyWarwickNotificationListenerComponent, EmailNotificationListenerComponent, MyWarwickNotificationListenerComponent}
import uk.ac.warwick.tabula.services.NotificationBatchingService._
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

object NotificationBatchingService {
  val RunBatchSize = 50
}

trait NotificationBatchingService {
  def processNotifications(): Unit

  def recentRecipients(start: Int, count: Int): Seq[RecipientNotificationInfo]
  def unemailedRecipientCount: Number
  def oldestUnemailedRecipient: Option[RecipientNotificationInfo]
  def recentEmailedRecipient: Option[RecipientNotificationInfo]
}

abstract class AbstractNotificationBatchingService extends NotificationBatchingService with Logging {
  self: NotificationDaoComponent
    with EmailNotificationListenerComponent
    with MyWarwickNotificationListenerComponent
    with UserSettingsServiceComponent
    with FeaturesComponent
    with SessionComponent =>

  // a map of DiscriminatorValue -> Notification
  lazy val notificationMap: Map[String, Class[_ <: Notification[ToEntityReference, Unit]]] = ReflectionHelper.allNotifications

  override def processNotifications(): Unit = transactional(readOnly = true) {
    // Get a list of *all* unemailed recipients, grouped by notification type and the min
    // created date of the email, to work out how many emails to send
    val futures: Seq[Future[(Seq[String], Try[Unit])]] =
      notificationDao.unemailedRecipientsByNotificationType(RunBatchSize).flatMap { case (recipient, notificationType, earliestCreatedDate) =>
        val batchedNotificationsSetting =
          userSettingsService.getByUserId(recipient.getUserId)
            .map(_.batchedNotifications)
            .getOrElse(UserSettings.DefaultBatchedNotificationsSetting)

        lazy val delay: FiniteDuration =
          Seconds.secondsBetween(earliestCreatedDate, DateTime.now).getSeconds.seconds

        lazy val canBeBatched: Boolean = features.notificationBatching && {
          val notificationClass = notificationMap(notificationType)
          val baseNotification: Notification[ToEntityReference, Unit] = notificationClass.newInstance()
          baseNotification.isInstanceOf[BatchedNotification[_]]
        }

        batchedNotificationsSetting match {
          case setting if !canBeBatched || setting.delay == Duration.Zero =>
            // Send each individually
            notificationDao.unemailedNotificationsFor(recipient, notificationType, retryBackoff = true).map { rni =>
              val id = rni.id
              Future {
                Seq(id) -> Try(transactional() {
                  // This is a (new) read-only session as it happens inside the Future
                  val r = session.load(classOf[RecipientNotificationInfo], id)
                  session.setReadOnly(r, false)

                  logger.info(s"Emailing recipient - $r")
                  emailNotificationListener.listen(r)
                  myWarwickNotificationListener.listen(r)
                })
              }
            }

          case setting if delay >= setting.delay =>
            // Send these as a batch
            val ids = notificationDao.unemailedNotificationsFor(recipient, notificationType, retryBackoff = false).map(_.id)
            Seq(Future {
              ids -> Try(transactional() {
                // This is a (new) read-only session as it happens inside the Future
                val recipients = ids.map(session.load(classOf[RecipientNotificationInfo], _))
                recipients.foreach(session.setReadOnly(_, false))

                logger.info(s"Emailing recipient batch - $recipients")
                emailNotificationListener.listenBatch(recipients)
                myWarwickNotificationListener.listenBatch(recipients)
              })
            })

          case _ =>
            // Too soon, ignore and let the next run handle it
            Nil
        }
      }

    Await.result(Future.sequence(futures), Duration.Inf).foreach { case (recipientId, result) =>
      result match {
        case Success(_) =>
        case Failure(throwable) =>
          // TAB-2238 Catch and log, so that the overall transaction can still commit
          logger.error(s"Exception handling email for RecipientNotificationInfo[$recipientId]:", throwable)
      }
    }
  }

  def recentRecipients(start: Int, count: Int): Seq[RecipientNotificationInfo] = notificationDao.recentRecipients(start, count)
  def unemailedRecipientCount: Number = notificationDao.unemailedRecipientCount
  def oldestUnemailedRecipient: Option[RecipientNotificationInfo] = notificationDao.oldestUnemailedRecipient
  def recentEmailedRecipient: Option[RecipientNotificationInfo] = notificationDao.recentEmailedRecipient
}

@Service("notificationBatchingService")
class AutowiringNotificationBatchingService
  extends AbstractNotificationBatchingService
    with AutowiringNotificationDaoComponent
    with AutowiringEmailNotificationListenerComponent
    with AutowiringMyWarwickNotificationListenerComponent
    with AutowiringUserSettingsServiceComponent
    with AutowiringFeaturesComponent
    with Daoisms
