package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.data.{Daoisms, NotificationDao, Scrollable}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.notifications.EmailNotificationListener
import uk.ac.warwick.tabula.helpers.Futures._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

@Service
class EmailNotificationService extends Logging with Daoisms {

	val RunBatchSize = 100

	var dao: NotificationDao = Wire[NotificationDao]
	var listener: RecipientNotificationListener = Wire[EmailNotificationListener]

	def processNotifications(): Unit = transactional() {
		val batch: Seq[RecipientNotificationInfo] = unemailedRecipients.take(RunBatchSize).toSeq
		val futures: Seq[Future[(RecipientNotificationInfo, Try[Unit])]] = batch.map { rni => Future {
			rni -> Try {
				// This is a (new) read-only session as it happens inside the Future
				val recipient = session.load(classOf[RecipientNotificationInfo], rni.id).asInstanceOf[RecipientNotificationInfo]
				session.setReadOnly(recipient, false)

				logger.info("Emailing recipient - " + recipient)
				listener.listen(recipient)
				session.flush()
			}
		}}

		Await.result(Future.sequence(futures), Duration.Inf).foreach { case (recipient, result) =>
			result match {
				case Success(_) =>
				case Failure(throwable) =>
					// TAB-2238 Catch and log, so that the overall transaction can still commit
					logger.error(s"Exception handling email for $recipient:", throwable)
			}
		}
	}

	def recentRecipients(start: Int, count: Int): Seq[RecipientNotificationInfo] = dao.recentRecipients(start, count)
	def unemailedRecipientCount: Number = dao.unemailedRecipientCount
	def unemailedRecipients: Scrollable[RecipientNotificationInfo] = dao.unemailedRecipients
	def oldestUnemailedRecipient: Option[RecipientNotificationInfo] = dao.oldestUnemailedRecipient
	def recentEmailedRecipient: Option[RecipientNotificationInfo] = dao.recentEmailedRecipient

}
