package uk.ac.warwick.tabula.services.healthchecks

import java.time.LocalDateTime

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.AuditEventService
import uk.ac.warwick.tabula.services.elasticsearch.AuditEventIndexService
import uk.ac.warwick.tabula.services.healthchecks.AuditIndexStatusHealthcheck._
import uk.ac.warwick.util.core.DateTimeUtils
import uk.ac.warwick.util.service.{ServiceHealthcheck, ServiceHealthcheckProvider}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object AuditIndexStatusHealthcheck {
  val Name = "audit-indexing"
  val InitialState = new ServiceHealthcheck(Name, ServiceHealthcheck.Status.Unknown, LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION))

  val WarningThreshold = 10 // minutes
  val ErrorThreshold = 15 // minutes
}

@Component
@Profile(Array("scheduling"))
class AuditIndexStatusHealthcheck extends ServiceHealthcheckProvider(InitialState) {

  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  def run(): Unit = transactional(readOnly = true) {
    val latestDb = Wire[AuditEventService].latest
    val latestIndex = Await.result(Wire[AuditEventIndexService].newestItemInIndexDate, 1.minute).getOrElse(new DateTime(0L))
    val latestIndexMinutesAgo = (latestDb.getMillis - latestIndex.getMillis) / (1000 * 60)

    val status =
      if (latestIndexMinutesAgo >= ErrorThreshold) ServiceHealthcheck.Status.Error
      else if (latestIndexMinutesAgo >= WarningThreshold) ServiceHealthcheck.Status.Warning
      else ServiceHealthcheck.Status.Okay

    update(new ServiceHealthcheck(
      Name,
      status,
      LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
      s"Last index $latestIndexMinutesAgo minute${if (latestIndexMinutesAgo == 1) "" else "s"} before last database (warning: $WarningThreshold, critical: $ErrorThreshold)",
      Seq[ServiceHealthcheck.PerformanceData[_]](
        new ServiceHealthcheck.PerformanceData("index_lag", latestIndexMinutesAgo, WarningThreshold, ErrorThreshold)
      ).asJava
    ))
  }

}
