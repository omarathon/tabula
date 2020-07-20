package uk.ac.warwick.tabula.services.healthchecks

import java.time.LocalDateTime

import humanize.Humanize._
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.services.elasticsearch.AuditEventQueryService
import uk.ac.warwick.tabula.services.healthchecks.ImportStudentAwardsStatusHealthcheck._
import uk.ac.warwick.util.core.DateTimeUtils
import uk.ac.warwick.util.service.{ServiceHealthcheck, ServiceHealthcheckProvider}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object ImportStudentAwardsStatusHealthcheck {
  val Name = "import-student-awards"
  val InitialState = new ServiceHealthcheck(Name, ServiceHealthcheck.Status.Unknown, LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION))
}

@Component
@Profile(Array("scheduling"))
class ImportStudentAwardsStatusHealthcheck
  extends ServiceHealthcheckProvider(InitialState) {

  /**
   * Fetch a list of audit events, most recent first, relating to this import
   */
  private def auditEvents: Seq[AuditEvent] = {
    val queryService = Wire[AuditEventQueryService]
    Await.result(queryService.query("eventType:BulkImportStudentAwardsForAcademicYear", 0, 50), 1.minute)
  }

  protected def getServiceHealthCheck(imports: Seq[AuditEvent]): ServiceHealthcheck = {
    //find the last successful import
    val lastSuccessful = imports.find(_.isSuccessful)

    // Find the last failed import if any
    val lastFailed = imports.find(_.hadError)

    val isError = lastFailed.isDefined && lastSuccessful.isDefined && lastFailed.get.eventDate.isAfter(lastSuccessful.get.eventDate)

    val status =
      if (isError)
        ServiceHealthcheck.Status.Error
      else
        ServiceHealthcheck.Status.Okay

    val successMessage =
      lastSuccessful.map { event => s"Last successful import ${naturalTime(event.eventDate.toDate)}" }


    val failedMessage =
      lastFailed.map { event => s"last import failed ${naturalTime(event.eventDate.toDate)}" }

    val message = Seq(
      successMessage.orElse(Some("No successful import found")),
      failedMessage
    ).flatten.mkString(", ")

    val lastSuccessfulHoursAgo: Double =
      lastSuccessful.map { event =>
        val d = new org.joda.time.Duration(event.eventDate, DateTime.now)
        d.toStandardSeconds.getSeconds / 3600.0
      }.getOrElse(0)

    new ServiceHealthcheck(
      Name,
      status,
      LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
      message,
      Seq[ServiceHealthcheck.PerformanceData[_]](
        new ServiceHealthcheck.PerformanceData("last_successful_hours", lastSuccessfulHoursAgo)
      ).asJava
    )
  }


  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  def run(): Unit = transactional(readOnly = true) {
    val imports = auditEvents

    update(getServiceHealthCheck(imports))
  }

}
