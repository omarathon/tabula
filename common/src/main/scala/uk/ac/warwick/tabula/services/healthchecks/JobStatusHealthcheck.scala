package uk.ac.warwick.tabula.services.healthchecks

import java.time.LocalDateTime

import humanize.Humanize._
import org.joda.time.{DateTime, Minutes}
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.util.core.DateTimeUtils
import uk.ac.warwick.util.service.{ServiceHealthcheck, ServiceHealthcheckProvider}

import scala.concurrent.duration._
import scala.concurrent.duration.Duration.Zero
import scala.jdk.CollectionConverters._

object JobStatusHealthcheck {
  val Name = "unfinished-jobs"
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
    val unfinishedJobs = service.unfinishedInstances.sortBy(_.createdDate)
    val oldestUnfinishedJob = unfinishedJobs.headOption
    val oldestUnfinishedJobDuration: Duration =
      oldestUnfinishedJob
        .map { job =>  Minutes.minutesBetween(job.createdDate, DateTime.now).getMinutes.minutes.toCoarsest
        }.getOrElse(Zero)

    val status = if (oldestUnfinishedJob.exists(_.createdDate.plusMillis(JobStatusHealthcheck.ErrorThreshold.toMillis.toInt).isBeforeNow))
      ServiceHealthcheck.Status.Error
    else if (oldestUnfinishedJob.exists(_.createdDate.plusMillis(JobStatusHealthcheck.WarningThreshold.toMillis.toInt).isBeforeNow))
      ServiceHealthcheck.Status.Warning
    else
      ServiceHealthcheck.Status.Okay

    val message =
      oldestUnfinishedJob.map { job => s"Oldest job created ${naturalTime(job.createdDate.toDate)}" }.getOrElse("There are no unfinished jobs.")


    update(new ServiceHealthcheck(
      JobStatusHealthcheck.Name,
      status,
      LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
      message,
      Seq[ServiceHealthcheck.PerformanceData[_]](
        new ServiceHealthcheck.PerformanceData("oldest_unfinished_jobs_created", oldestUnfinishedJobDuration.toMinutes, JobStatusHealthcheck.WarningThreshold.toHours, JobStatusHealthcheck.ErrorThreshold.toHours)
      ).asJava
    ))

  }
}
