package uk.ac.warwick.tabula.services.healthchecks

import java.time.LocalDateTime

import org.joda.time.{DateTime, Minutes}
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.healthchecks.ExportRecordedModuleRegistrationsToSitsHealthcheck._
import uk.ac.warwick.tabula.services.marks.ModuleRegistrationMarksService
import uk.ac.warwick.util.core.DateTimeUtils
import uk.ac.warwick.util.service.{ServiceHealthcheck, ServiceHealthcheckProvider}

import scala.concurrent.duration.Duration.Zero
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object ExportRecordedModuleRegistrationsToSitsHealthcheck {
  val Name = "export-module-marks-to-sits"
  val InitialState = new ServiceHealthcheck(Name, ServiceHealthcheck.Status.Unknown, LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION))

  val QueueSizeWarningThreshold = 1000
  val QueueSizeErrorThreshold = 2000

  val DelayWarningThreshold: Duration = 30.minutes
  val DelayErrorThreshold: Duration = 1.hour
}

@Component
@Profile(Array("scheduling"))
class ExportRecordedModuleRegistrationsToSitsHealthcheck extends ServiceHealthcheckProvider(InitialState) {

  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  def run(): Unit = transactional(readOnly = true) {
    val service = Wire[ModuleRegistrationMarksService]

    // Don't consider any that aren't allowed
    val queue = service.allNeedingWritingToSits(filtered = true)

    val queueSize = queue.size

    val countStatus =
      if (queueSize >= QueueSizeErrorThreshold) ServiceHealthcheck.Status.Error
      else if (queueSize >= QueueSizeWarningThreshold) ServiceHealthcheck.Status.Warning
      else ServiceHealthcheck.Status.Okay

    val countMessage =
      s"$queueSize mark${if (queueSize == 1) "" else "s"} in queue" +
      (if (countStatus == ServiceHealthcheck.Status.Error) " (!!)" else if (countStatus == ServiceHealthcheck.Status.Warning) " (!)" else "") +
      s" (warning: $QueueSizeWarningThreshold, critical: $QueueSizeErrorThreshold)"

    // How old is the oldest item in the queue?
    val oldestUnwrittenMarkDelay =
      queue.flatMap(_.marks.headOption).minByOption(_.updatedDate).map { mark =>
        Minutes.minutesBetween(mark.updatedDate, DateTime.now).getMinutes.minutes.toCoarsest
      }.getOrElse(Zero)

    val mostRecentlyWrittenMarkDelay =
      service.mostRecentlyWrittenToSitsDate.map { syncDate =>
        Minutes.minutesBetween(syncDate, DateTime.now).getMinutes.minutes.toCoarsest
      }.getOrElse(Zero)

    val delayStatus =
      if (oldestUnwrittenMarkDelay == Zero) ServiceHealthcheck.Status.Okay // empty queue
      else if (mostRecentlyWrittenMarkDelay >= DelayErrorThreshold) ServiceHealthcheck.Status.Error
      else if (mostRecentlyWrittenMarkDelay >= DelayWarningThreshold) ServiceHealthcheck.Status.Warning
      else ServiceHealthcheck.Status.Okay // queue still processing so may take time to sent them all

    val delayMessage =
      s"Last written mark $mostRecentlyWrittenMarkDelay ago, oldest unwritten mark $oldestUnwrittenMarkDelay old " +
      (if (delayStatus == ServiceHealthcheck.Status.Error) " (!!)" else if (delayStatus == ServiceHealthcheck.Status.Warning) " (!)" else "") +
      s"(warning: $DelayWarningThreshold, critical: $DelayErrorThreshold)"

    val status = Seq(countStatus, delayStatus).maxBy(_.ordinal())

    update(new ServiceHealthcheck(
      Name,
      status,
      LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
      s"$countMessage. $delayMessage",
      Seq[ServiceHealthcheck.PerformanceData[_]](
        new ServiceHealthcheck.PerformanceData("queue_size", queueSize, QueueSizeWarningThreshold, QueueSizeErrorThreshold),
        new ServiceHealthcheck.PerformanceData("oldest_written", oldestUnwrittenMarkDelay.toMinutes, DelayWarningThreshold.toMinutes, DelayErrorThreshold.toMinutes),
        new ServiceHealthcheck.PerformanceData("last_written", mostRecentlyWrittenMarkDelay.toMinutes, DelayWarningThreshold.toMinutes, DelayErrorThreshold.toMinutes)
      ).asJava
    ))
  }

}
