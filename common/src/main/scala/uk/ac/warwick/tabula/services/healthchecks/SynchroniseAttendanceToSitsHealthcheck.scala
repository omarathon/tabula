package uk.ac.warwick.tabula.services.healthchecks

import java.time.LocalDateTime

import org.joda.time.{DateTime, Minutes}
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.services.healthchecks.SynchroniseAttendanceToSitsHealthcheck._
import uk.ac.warwick.util.core.DateTimeUtils
import uk.ac.warwick.util.service.{ServiceHealthcheck, ServiceHealthcheckProvider}

import scala.concurrent.duration.Duration.Zero
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object SynchroniseAttendanceToSitsHealthcheck {
  val Name = "sync-attendance-to-sits"
  val InitialState = new ServiceHealthcheck(Name, ServiceHealthcheck.Status.Unknown, LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION))

  val UnsynchronisedCountWarningThreshold = 1000
  val UnsynchronisedCountErrorThreshold = 10000

  val DelayWarningThreshold: Duration = 30.minutes
  val DelayErrorThreshold: Duration = 1.hour
}

@Component
@Profile(Array("scheduling"))
class SynchroniseAttendanceToSitsHealthcheck extends ServiceHealthcheckProvider(InitialState) {

  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  def run(): Unit = transactional(readOnly = true) {
    val service = Wire[AttendanceMonitoringService]

    val unsynchronisedCheckpointCount = service.countUnsynchronisedCheckpoints

    val countStatus =
      if (unsynchronisedCheckpointCount >= UnsynchronisedCountErrorThreshold) ServiceHealthcheck.Status.Error
      else if (unsynchronisedCheckpointCount >= UnsynchronisedCountWarningThreshold) ServiceHealthcheck.Status.Warning
      else ServiceHealthcheck.Status.Okay

    val countMessage =
      s"$unsynchronisedCheckpointCount checkpoint${if (unsynchronisedCheckpointCount == 1) "" else "s"} in queue" +
      (if (countStatus == ServiceHealthcheck.Status.Error) " (!!)" else if (countStatus == ServiceHealthcheck.Status.Warning) " (!)" else "") +
      s" (warning: $UnsynchronisedCountWarningThreshold, critical: $UnsynchronisedCountErrorThreshold)"

    // How old is the oldest item in the queue?
    val oldestUnsynchronisedCheckpointDelay =
      service.listUnsynchronisedCheckpoints(1).headOption.map { checkpoint =>
        Minutes.minutesBetween(checkpoint.updatedDate, DateTime.now).getMinutes.minutes.toCoarsest
      }.getOrElse(Zero)

    val mostRecentlySynchronisedCheckpointDelay =
      service.mostRecentlySynchronisedCheckpointDate.map { syncDate =>
        Minutes.minutesBetween(syncDate, DateTime.now).getMinutes.minutes.toCoarsest
      }.getOrElse(Zero)

    val delayStatus =
      if (oldestUnsynchronisedCheckpointDelay == Zero) ServiceHealthcheck.Status.Okay // empty queue
      else if (mostRecentlySynchronisedCheckpointDelay >= DelayErrorThreshold) ServiceHealthcheck.Status.Error
      else if (mostRecentlySynchronisedCheckpointDelay >= DelayWarningThreshold) ServiceHealthcheck.Status.Warning
      else ServiceHealthcheck.Status.Okay // queue still processing so may take time to sent them all

    val delayMessage =
      s"Last synchronised checkpoint $mostRecentlySynchronisedCheckpointDelay ago, oldest unsynchronised checkpoint $oldestUnsynchronisedCheckpointDelay old " +
      (if (delayStatus == ServiceHealthcheck.Status.Error) " (!!)" else if (delayStatus == ServiceHealthcheck.Status.Warning) " (!)" else "") +
      s"(warning: $DelayWarningThreshold, critical: $DelayErrorThreshold)"

    val status = Seq(countStatus, delayStatus).maxBy(_.ordinal())

    update(new ServiceHealthcheck(
      Name,
      status,
      LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
      s"$countMessage. $delayMessage",
      Seq[ServiceHealthcheck.PerformanceData[_]](
        new ServiceHealthcheck.PerformanceData("queue_size", unsynchronisedCheckpointCount, UnsynchronisedCountWarningThreshold, UnsynchronisedCountErrorThreshold),
        new ServiceHealthcheck.PerformanceData("oldest_unsynchronised", oldestUnsynchronisedCheckpointDelay.toMinutes, DelayWarningThreshold.toMinutes, DelayErrorThreshold.toMinutes),
        new ServiceHealthcheck.PerformanceData("last_synchronised", mostRecentlySynchronisedCheckpointDelay.toMinutes, DelayWarningThreshold.toMinutes, DelayErrorThreshold.toMinutes)
      ).asJava
    ))
  }

}
