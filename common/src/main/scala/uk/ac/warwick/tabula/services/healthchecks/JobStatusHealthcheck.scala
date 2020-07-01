package uk.ac.warwick.tabula.services.healthchecks

import java.time.LocalDateTime

import humanize.Humanize._
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.util.core.DateTimeUtils
import uk.ac.warwick.util.service.{ServiceHealthcheck, ServiceHealthcheckProvider}

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object JobStatusHealthcheck {
  val Name = "pending-jobs"
  val InitialState = new ServiceHealthcheck(Name, ServiceHealthcheck.Status.Unknown, LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION))

  val WarningThreshold: Duration = 30.minutes
  val ErrorThreshold: Duration = 1.hour
}


@Component
@Profile(Array("scheduling"))
class JobStatusHealthcheck
  extends ServiceHealthcheckProvider(JobStatusHealthcheck.InitialState) {

  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  override def run(): Unit = {

    val service = Wire[JobService]
    val pendingJobs = service.unfinishedInstances.filter(_.started == false).sortBy(_.createdDate)
    val oldestPendingJob = pendingJobs.headOption
    val status = if (oldestPendingJob.exists(_.createdDate.plusMillis(JobStatusHealthcheck.ErrorThreshold.toMillis.toInt).isBeforeNow))
      ServiceHealthcheck.Status.Error
    else if (oldestPendingJob.exists(_.createdDate.plusMillis(JobStatusHealthcheck.WarningThreshold.toMillis.toInt).isBeforeNow))
      ServiceHealthcheck.Status.Warning
    else
      ServiceHealthcheck.Status.Okay

    val message =
      oldestPendingJob.map { job => s"Oldest job created ${naturalTime(job.createdDate.toDate)}" }.getOrElse("There are no pending jobs that needs starting.")


    update(new ServiceHealthcheck(
      JobStatusHealthcheck.Name,
      status,
      LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
      message,
      Seq[ServiceHealthcheck.PerformanceData[_]](
        new ServiceHealthcheck.PerformanceData("pending_jobs_total", pendingJobs.size, JobStatusHealthcheck.WarningThreshold.toHours, JobStatusHealthcheck.ErrorThreshold.toHours)
      ).asJava
    ))

  }
}

