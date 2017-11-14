package uk.ac.warwick.tabula.services.healthchecks

import org.joda.time.{DateTime, Minutes}
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.services.EmailNotificationService

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

	val WarningThreshold = 5 // minutes
	val ErrorThreshold = 10 // minutes

	@Scheduled(fixedRate = 60 * 1000) // 1 minute
	def run(): Unit = transactional(readOnly = true) {
		// How old (in minutes) is the oldest item in the queue?
		val oldestUnsentEmail =
			Wire[EmailNotificationService].oldestUnemailedRecipient
				.map { recipient: RecipientNotificationInfo =>
				Minutes.minutesBetween(recipient.notification.created, DateTime.now).getMinutes
			}.getOrElse(0)

		// How new (in minutes) is the latest item in the queue?
		val recentSentEmail =
			Wire[EmailNotificationService].recentEmailedRecipient
				.map { recipient: RecipientNotificationInfo =>
					Minutes.minutesBetween(recipient.notification.created, DateTime.now).getMinutes
				}.getOrElse(0)

		val status =
			if (oldestUnsentEmail == 0) ServiceHealthcheck.Status.Okay // empty queue
			else if (recentSentEmail >= ErrorThreshold) ServiceHealthcheck.Status.Error
			else if (recentSentEmail >= WarningThreshold) ServiceHealthcheck.Status.Warning
			else ServiceHealthcheck.Status.Okay // email queue still processing so may take time to sent them all

		update(ServiceHealthcheck(
			name = "email-delay",
			status = status,
			testedAt = DateTime.now,
			message = s"Last sent email $recentSentEmail minute${if (recentSentEmail == 1) "" else "s"} old, Oldest unsent email $oldestUnsentEmail minute${if (oldestUnsentEmail == 1) "" else "s"} old, (warning: $WarningThreshold, critical: $ErrorThreshold)",
			performanceData = Seq(
				ServiceHealthcheck.PerformanceData("oldest_unsent", oldestUnsentEmail, WarningThreshold, ErrorThreshold),
				ServiceHealthcheck.PerformanceData("last_sent", recentSentEmail, WarningThreshold, ErrorThreshold),
			)
		))
	}

}
