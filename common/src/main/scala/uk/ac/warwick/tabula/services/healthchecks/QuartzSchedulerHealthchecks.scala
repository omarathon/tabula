package uk.ac.warwick.tabula.services.healthchecks

import java.sql.ResultSet
import java.time.LocalDateTime

import javax.sql.DataSource
import org.joda.time.DateTime
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.healthchecks.QuartzJdbc._
import uk.ac.warwick.util.core.DateTimeUtils
import uk.ac.warwick.util.core.scheduling.QuartzDAO
import uk.ac.warwick.util.service.healthchecks.scheduling.{AbstractQuartzJobQueueHealthcheck, AbstractQuartzSchedulerHealthcheck}
import uk.ac.warwick.util.service.{ServiceHealthcheck, ServiceHealthcheckProvider}
import uk.ac.warwick.util.web.Uri

import scala.jdk.CollectionConverters._

object QuartzSchedulerTriggersHealthcheck {
  val name = "quartz-triggers"
  val initialState = new ServiceHealthcheck(name, ServiceHealthcheck.Status.Unknown, LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION))
}

@Component
@Profile(Array("scheduling"))
class QuartzSchedulerTriggersHealthcheck
  extends ServiceHealthcheckProvider(QuartzSchedulerTriggersHealthcheck.initialState) {

  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  override def run(): Unit = {
    val jdbcTemplate = new JdbcTemplate(Wire.named[DataSource]("dataSource"))
    val clusterName = Uri.parse(Wire.property("${toplevel.url}")).getAuthority

    val allTriggers =
      jdbcTemplate.queryAndMap("select * from qrtz_triggers") {
        case (resultSet, _) => QuartzJdbc.Trigger(resultSet)
      }.filter(_.clusterName == clusterName)

    val allFiredTriggers =
      jdbcTemplate.queryAndMap("select * from qrtz_fired_triggers") {
        case (resultSet, _) => QuartzJdbc.FiredTrigger(resultSet)
      }.filter(_.clusterName == clusterName)

    val triggers = allTriggers.map { trigger =>
      val firedTrigger = allFiredTriggers.find(_.name == trigger.name)
      if (firedTrigger.nonEmpty)
        trigger.copy(state = if (firedTrigger.exists(_.executing)) TriggerState.Executing else TriggerState.Acquired)
      else trigger
    }.groupBy(_.state)

    val status =
      if (triggers.get(TriggerState.Error).exists(_.nonEmpty)) ServiceHealthcheck.Status.Error
      else ServiceHealthcheck.Status.Okay

    val statusString =
      TriggerState.members.reverse.flatMap { state =>
        triggers.get(state).map { triggers =>
          s"$state: ${triggers.length} (${triggers.map(_.name).mkString(", ")})"
        }
      }.mkString(", ")

    val perfData: Seq[ServiceHealthcheck.PerformanceData[_]] = TriggerState.members.flatMap {
      case state @ (TriggerState.Error | TriggerState.Blocked) =>
        triggers.get(state).map { triggers =>
          new ServiceHealthcheck.PerformanceData(state.dbValue.toLowerCase, triggers.length, 0, 1)
        }
      case state => triggers.get(state).map { triggers =>
        new ServiceHealthcheck.PerformanceData(state.dbValue.toLowerCase, triggers.length)
      }
    }

    update(new ServiceHealthcheck(
      QuartzSchedulerTriggersHealthcheck.name,
      status,
      LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
      statusString,
      perfData.asJava
    ))
  }

}

@Component
@Profile(Array("scheduling"))
@Autowired
class QuartzSchedulerClusterHealthcheck(dao: QuartzDAO, scheduler: Scheduler)
  extends AbstractQuartzSchedulerHealthcheck(
    "quartz-cluster",
    dao,
    () => scheduler.getSchedulerName
  ) {

  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  override def run(): Unit = transactional(readOnly = true) { update(status()) }
}

@Component
@Profile(Array("scheduling"))
@Autowired
class QuartzJobQueueHealthcheck(dao: QuartzDAO, scheduler: Scheduler)
  extends AbstractQuartzJobQueueHealthcheck(
    "quartz-job-queue",
    dao,
    () => scheduler.getSchedulerName
  ) {

  // My Warwick jobs bump this up massively
  setSizeWarningThreshold(1000)
  setSizeCriticalThreshold(10000)

  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  override def run(): Unit = transactional(readOnly = true) { update(status()) }
}

object QuartzJdbc {

  sealed abstract class TriggerState(val dbValue: String)

  object TriggerState {

    // WAITING, ACQUIRED, COMPLETE, PAUSED, BLOCKED, PAUSED_BLOCKED, ERROR

    case object Waiting extends TriggerState("WAITING")

    case object Acquired extends TriggerState("ACQUIRED")

    case object Executing extends TriggerState("EXECUTING")

    case object Complete extends TriggerState("COMPLETE")

    case object Paused extends TriggerState("PAUSED")

    case object Blocked extends TriggerState("BLOCKED")

    case object PausedBlocked extends TriggerState("PAUSED_BLOCKED")

    case object Error extends TriggerState("ERROR")

    // lame manual collection. Keep in sync with the case objects above.
    // less severe at the start, more severe at the end
    val members = Seq(Waiting, Acquired, Executing, Complete, Paused, Blocked, PausedBlocked, Error)

    def unapply(dbValue: String): Option[TriggerState] = members.find {
      _.dbValue == dbValue
    }

    def apply(dbValue: String): TriggerState = dbValue match {
      case TriggerState(t) => t
      case _ => throw new IllegalArgumentException(dbValue)
    }
  }

  case class Trigger(clusterName: String, name: String, jobName: String, lastFired: Option[DateTime], nextFire: DateTime, state: TriggerState)

  object Trigger {
    def apply(rs: ResultSet): Trigger = Trigger(
      clusterName = rs.getString("sched_name"),
      name = rs.getString("trigger_name"),
      jobName = rs.getString("job_name"),
      lastFired = rs.getLong("prev_fire_time") match {
        case -1 => None
        case millis => Some(new DateTime(millis))
      },
      nextFire = new DateTime(rs.getLong("next_fire_time")),
      state = TriggerState(rs.getString("trigger_state"))
    )
  }

  case class FiredTrigger(clusterName: String, name: String, instance: String, fired: DateTime, scheduled: DateTime, priority: Int, executing: Boolean, jobName: String, nonConcurrent: Boolean, requestsRecovery: Boolean)

  object FiredTrigger {
    def apply(rs: ResultSet): FiredTrigger = FiredTrigger(
      clusterName = rs.getString("sched_name"),
      name = rs.getString("trigger_name"),
      instance = rs.getString("instance_name"),
      fired = new DateTime(rs.getLong("fired_time")),
      scheduled = new DateTime(rs.getLong("sched_time")),
      priority = rs.getInt("priority"),
      executing = rs.getString("state") == "EXECUTING",
      jobName = rs.getString("job_name"),
      nonConcurrent = rs.getBoolean("is_nonconcurrent"),
      requestsRecovery = rs.getBoolean("requests_recovery")
    )
  }

}
