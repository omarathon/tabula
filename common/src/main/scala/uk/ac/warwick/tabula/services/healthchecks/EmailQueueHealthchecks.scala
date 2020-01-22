package uk.ac.warwick.tabula.services.healthchecks

import java.time.LocalDateTime

import org.joda.time.{DateTime, Minutes}
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.NotificationBatchingService
import uk.ac.warwick.util.core.DateTimeUtils
import uk.ac.warwick.util.service.{ServiceHealthcheck, ServiceHealthcheckProvider}

import scala.concurrent.duration.Duration.Zero
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object EmailUnsentEmailCountHealthcheck {
  val Name = "email-queue"
  val InitialState = new ServiceHealthcheck(Name, ServiceHealthcheck.Status.Unknown, LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION))

  val WarningThreshold = 10000
  val ErrorThreshold = 25000
}

@Component
@Profile(Array("scheduling"))
class EmailUnsentEmailCountHealthcheck extends ServiceHealthcheckProvider(EmailUnsentEmailCountHealthcheck.InitialState) {

  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  def run(): Unit = transactional(readOnly = true) {
    // Number of unsent emails in queue
    val unsentEmailCount = Wire[NotificationBatchingService].unemailedRecipientCount.intValue()

    val status =
      if (unsentEmailCount >= EmailUnsentEmailCountHealthcheck.ErrorThreshold) ServiceHealthcheck.Status.Error
      else if (unsentEmailCount >= EmailUnsentEmailCountHealthcheck.WarningThreshold) ServiceHealthcheck.Status.Warning
      else ServiceHealthcheck.Status.Okay

    update(new ServiceHealthcheck(
      EmailUnsentEmailCountHealthcheck.Name,
      status,
      LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
      s"$unsentEmailCount item${if (unsentEmailCount == 1) "" else "s"} in queue (warning: ${EmailUnsentEmailCountHealthcheck.WarningThreshold}, critical: ${EmailUnsentEmailCountHealthcheck.ErrorThreshold})",
      Seq[ServiceHealthcheck.PerformanceData[_]](
        new ServiceHealthcheck.PerformanceData("queue_size", unsentEmailCount, EmailUnsentEmailCountHealthcheck.WarningThreshold, EmailUnsentEmailCountHealthcheck.ErrorThreshold)
      ).asJava
    ))
  }

}

object EmailOldestUnsentItemHealthcheck {
  val Name = "email-delay"
  val InitialState = new ServiceHealthcheck(Name, ServiceHealthcheck.Status.Unknown, LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION))

  val WarningThreshold: Duration = 30.minutes
  val ErrorThreshold: Duration = 1.hour
}

@Component
@Profile(Array("scheduling"))
class EmailOldestUnsentItemHealthcheck extends ServiceHealthcheckProvider(EmailOldestUnsentItemHealthcheck.InitialState) with Logging {

  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  def run(): Unit = transactional(readOnly = true) {
    // How old is the oldest item in the queue?
    val oldestUnsentEmail: Duration =
      Wire[NotificationBatchingService].oldestUnemailedRecipient
        .map { recipient: RecipientNotificationInfo =>
          Minutes.minutesBetween(recipient.notification.created, DateTime.now).getMinutes.minutes.toCoarsest
        }.getOrElse(Zero)

    // How new is the latest item in the queue?
    val recentSentEmail: Duration =
      Wire[NotificationBatchingService].recentEmailedRecipient
        .filter(_.attemptedAt != null) // See TAB-6033 for details
        .map { recipient: RecipientNotificationInfo =>
        Minutes.minutesBetween(recipient.attemptedAt, DateTime.now).getMinutes.minutes.toCoarsest
      }.getOrElse(Zero)

    val status =
      if (oldestUnsentEmail == Zero) ServiceHealthcheck.Status.Okay // empty queue
      else if (recentSentEmail >= EmailOldestUnsentItemHealthcheck.ErrorThreshold) ServiceHealthcheck.Status.Error
      else if (recentSentEmail >= EmailOldestUnsentItemHealthcheck.WarningThreshold) ServiceHealthcheck.Status.Warning
      else ServiceHealthcheck.Status.Okay // email queue still processing so may take time to sent them all

    update(new ServiceHealthcheck(
      EmailOldestUnsentItemHealthcheck.Name,
      status,
      LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
      s"Last sent email $recentSentEmail ago, oldest unsent email $oldestUnsentEmail old, (warning: ${EmailOldestUnsentItemHealthcheck.WarningThreshold}, critical: ${EmailOldestUnsentItemHealthcheck.ErrorThreshold})",
      Seq[ServiceHealthcheck.PerformanceData[_]](
        new ServiceHealthcheck.PerformanceData("oldest_unsent", oldestUnsentEmail.toMinutes, EmailOldestUnsentItemHealthcheck.WarningThreshold.toMinutes, EmailOldestUnsentItemHealthcheck.ErrorThreshold.toMinutes),
        new ServiceHealthcheck.PerformanceData("last_sent", recentSentEmail.toMinutes, EmailOldestUnsentItemHealthcheck.WarningThreshold.toMinutes, EmailOldestUnsentItemHealthcheck.ErrorThreshold.toMinutes)
      ).asJava
    ))
  }

}
