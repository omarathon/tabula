package uk.ac.warwick.tabula.services.healthchecks

import org.joda.time.{DateTime, Minutes}
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.services.EmailNotificationService

import scala.concurrent.duration._
import scala.concurrent.duration.Duration.Zero

@Component
@Profile(Array("scheduling"))
class EmailUnsentEmailCountHealthcheck extends ServiceHealthcheckProvider {

	val WarningThreshold = 1000
	val ErrorThreshold = 10000

	@Scheduled(fixedRate = 60 * 1000) // 1 minute
	def run(): Unit = transactional(readOnly = true) {
		// Number of unsent emails in queue
		val unsentEmailCount = Wire[EmailNotificationService].unemailedRecipientCount.intValue()

		val status =
			if (unsentEmailCount >= ErrorThreshold) ServiceHealthcheck.Status.Error
			else if (unsentEmailCount >= WarningThreshold) ServiceHealthcheck.Status.Warning
			else ServiceHealthcheck.Status.Okay

		update(ServiceHealthcheck(
			name = "email-queue",
			status = status,
			testedAt = DateTime.now,
			message = s"$unsentEmailCount item${if (unsentEmailCount == 1) "" else "s"} in queue (warning: $WarningThreshold, critical: $ErrorThreshold)",
			performanceData = Seq(
				ServiceHealthcheck.PerformanceData("queue_size", unsentEmailCount, WarningThreshold, ErrorThreshold)
			)
		))
	}

}

@Component
@Profile(Array("scheduling"))
class EmailOldestUnsentItemHealthcheck extends ServiceHealthcheckProvider {

	val WarningThreshold: Duration = 30.minutes
	val ErrorThreshold: Duration = 1.hour

	@Scheduled(fixedRate = 60 * 1000) // 1 minute
	def run(): Unit = transactional(readOnly = true) {
		// How old is the oldest item in the queue?
		val oldestUnsentEmail: Duration =
			Wire[EmailNotificationService].oldestUnemailedRecipient
				.map { recipient: RecipientNotificationInfo =>
					Minutes.minutesBetween(recipient.notification.created, DateTime.now).getMinutes.minutes.toCoarsest
				}.getOrElse(Zero)

		// How new is the latest item in the queue?
		val recentSentEmail: Duration =
			Wire[EmailNotificationService].recentEmailedRecipient
				.map { recipient: RecipientNotificationInfo =>
					Minutes.minutesBetween(recipient.attemptedAt, DateTime.now).getMinutes.minutes.toCoarsest
				}.getOrElse(Zero)

		val status =
			if (oldestUnsentEmail == Zero) ServiceHealthcheck.Status.Okay // empty queue
			else if (recentSentEmail >= ErrorThreshold) ServiceHealthcheck.Status.Error
			else if (recentSentEmail >= WarningThreshold) ServiceHealthcheck.Status.Warning
			else ServiceHealthcheck.Status.Okay // email queue still processing so may take time to sent them all

		update(ServiceHealthcheck(
			name = "email-delay",
			status = status,
			testedAt = DateTime.now,
			message = s"Last sent email $recentSentEmail ago, oldest unsent email $oldestUnsentEmail old, (warning: $WarningThreshold, critical: $ErrorThreshold)",
			performanceData = Seq(
				ServiceHealthcheck.PerformanceData("oldest_unsent", oldestUnsentEmail.toMinutes, WarningThreshold.toMinutes, ErrorThreshold.toMinutes),
				ServiceHealthcheck.PerformanceData("last_sent", recentSentEmail.toMinutes, WarningThreshold.toMinutes, ErrorThreshold.toMinutes)
			)
		))
	}

}
