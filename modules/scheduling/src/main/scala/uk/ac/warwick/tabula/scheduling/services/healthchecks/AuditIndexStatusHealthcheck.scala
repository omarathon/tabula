package uk.ac.warwick.tabula.scheduling.services.healthchecks

import org.joda.time.DateTime
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.AuditEventService
import uk.ac.warwick.tabula.services.elasticsearch.AuditEventIndexService
import uk.ac.warwick.tabula.services.healthchecks.{ServiceHealthcheck, ServiceHealthcheckProvider}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

@Component
class AuditIndexStatusHealthcheck extends ServiceHealthcheckProvider {

	val WarningThreshold = 10 // minutes
	val ErrorThreshold = 15 // minutes

	@Scheduled(fixedRate = 60 * 1000) // 1 minute
	def run(): Unit = transactional(readOnly = true) {
		val latestDb = Wire[AuditEventService].latest
		val latestIndex = Try(Await.result(Wire[AuditEventIndexService].newestItemInIndexDate, 10.seconds).getOrElse(new DateTime(0L))).getOrElse(new DateTime(0L))
		val latestIndexMinutesAgo = (latestDb.getMillis - latestIndex.getMillis) / (1000 * 60)

		val status =
			if (latestIndexMinutesAgo >= ErrorThreshold) ServiceHealthcheck.Status.Error
			else if (latestIndexMinutesAgo >= WarningThreshold) ServiceHealthcheck.Status.Warning
			else ServiceHealthcheck.Status.Okay

		update(ServiceHealthcheck(
			name = "audit-indexing",
			status = status,
			testedAt = DateTime.now,
			message = s"Last index $latestIndexMinutesAgo minute${if (latestIndexMinutesAgo == 1) "" else "s"} before last database (warning: $WarningThreshold, critical: $ErrorThreshold)",
			performanceData = Seq(
				ServiceHealthcheck.PerformanceData("index_lag", latestIndexMinutesAgo, WarningThreshold, ErrorThreshold)
			)
		))
	}

}
