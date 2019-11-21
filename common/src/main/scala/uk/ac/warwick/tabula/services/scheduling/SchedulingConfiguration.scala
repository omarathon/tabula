package uk.ac.warwick.tabula.services.scheduling

import java.lang.{Integer => JInteger, Boolean => JBoolean}
import java.util.Properties
import java.util.concurrent.TimeUnit

import javax.sql.DataSource
import org.quartz._
import org.springframework.beans.factory.annotation.{Autowired, Qualifier, Value}
import org.springframework.beans.factory.{FactoryBean, InitializingBean}
import org.springframework.context.annotation.{Bean, Configuration, Profile}
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.env.{Environment, Profiles, PropertyResolver, PropertySourcesPropertyResolver}
import org.springframework.core.io.ClassPathResource
import org.springframework.scala.jdbc.core.JdbcTemplate
import org.springframework.scheduling.quartz.{JobDetailFactoryBean, QuartzJobBean, SchedulerFactoryBean, SpringBeanJobFactory}
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.SchedulingHelpers._
import uk.ac.warwick.tabula.services.MaintenanceModeService
import uk.ac.warwick.tabula.services.scheduling.SchedulingConfiguration.{JobConfiguration, ScheduledJob}
import uk.ac.warwick.tabula.services.scheduling.jobs._
import uk.ac.warwick.tabula.system.exceptions.ExceptionResolver
import uk.ac.warwick.util.core.spring.scheduling.{AutowiringSpringBeanJobFactory, PersistableCronTriggerFactoryBean, PersistableSimpleTriggerFactoryBean}
import uk.ac.warwick.util.web.Uri

import scala.concurrent.duration._
import scala.language.existentials
import scala.reflect._

object SchedulingConfiguration {

  sealed abstract class JobConfiguration[J <: AutowiredJobBean : ClassTag] {
    def name: String

    lazy val jobDetail: JobDetail = {
      val jobDetail = new JobDetailFactoryBean
      jobDetail.setName(name)
      jobDetail.setJobClass(classTag[J].runtimeClass.asInstanceOf[Class[J]])
      jobDetail.setDurability(true)
      jobDetail.setRequestsRecovery(true)
      jobDetail.afterPropertiesSet()
      jobDetail.getObject
    }
  }

  case class SimpleUnscheduledJob[J <: AutowiredJobBean : ClassTag](name: String) extends JobConfiguration[J]

  sealed abstract class ScheduledJob[J <: AutowiredJobBean : ClassTag, T <: Trigger] extends JobConfiguration[J] {
    def trigger: T
  }

  case class SimpleTriggerJob[J <: AutowiredJobBean : ClassTag](
    repeatInterval: Duration,
    name: String,
    misfireInstruction: Int = SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT
  ) extends ScheduledJob[J, SimpleTrigger] {
    lazy val trigger: SimpleTrigger = {
      val trigger = new PersistableSimpleTriggerFactoryBean
      trigger.setName(name)
      trigger.setJobDetail(jobDetail)
      if (repeatInterval > 0.seconds) {
        trigger.setRepeatInterval(repeatInterval.toMillis)
      } else {
        trigger.setRepeatInterval(1.minute.toMillis) // Have to set something or it defaults to zero and throws
        trigger.setRepeatCount(0)
      }
      trigger.setMisfireInstruction(misfireInstruction)
      trigger.afterPropertiesSet()
      trigger.getObject
    }
  }

  case class CronTriggerJob[J <: AutowiredJobBean : ClassTag](
    cronExpression: String,
    name: String,
    misfireInstruction: Int = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING
  ) extends ScheduledJob[J, CronTrigger] {
    lazy val trigger: CronTrigger = {
      val trigger = new PersistableCronTriggerFactoryBean
      trigger.setName(name)
      trigger.setJobDetail(jobDetail)
      trigger.setCronExpression(cronExpression)
      trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING)
      trigger.afterPropertiesSet()
      trigger.getObject
    }
  }

  /**
   * Parses a duration string. If no units are specified in the string, it is
   * assumed to be in milliseconds. The returned duration is in nanoseconds.
   * The purpose of this function is to implement the duration-related methods
   * in the ConfigObject interface.
   *
   * From Typesafe SimpleConfig
   *
   * @param input
   *              the string to parse
   * @return duration as a Scala FiniteDuration
   */
  private def parseDuration(input: String): FiniteDuration = {
    val unitStringRaw: String = input.reverse.takeWhile(Character.isLetter).reverse
    val numberString: String = input.substring(0, input.length - (unitStringRaw.length + 1))

    require(numberString.length > 0, s"No number in period value '$input'")

    val unitString =
      if (unitStringRaw.length > 2 && !unitStringRaw.endsWith("s")) s"${unitStringRaw}s"
      else unitStringRaw

    // Note that this is deliberately case-sensitive
    val unit: TimeUnit = unitString match {
      case "" | "ms" | "millis" | "milliseconds" => TimeUnit.MILLISECONDS
      case "us" | "micros" | "microseconds" => TimeUnit.MICROSECONDS
      case "ns" | "nanos" | "nanoseconds" => TimeUnit.NANOSECONDS
      case "d" | "days" => TimeUnit.DAYS
      case "h" | "hours" => TimeUnit.HOURS
      case "s" | "seconds" => TimeUnit.SECONDS
      case "m" | "minutes" => TimeUnit.MINUTES
      case _ => throw new IllegalArgumentException(s"Could not parse time unit '$unitStringRaw' (try ns, us, ms, s, m, h, d)")
    }

    Duration(numberString.toLong, unit)
  }

  private def defaultJobName[J <: AutowiredJobBean : ClassTag]: String = classTag[J].runtimeClass.getSimpleName

  private def propertiesConfiguredScheduledJob[J <: AutowiredJobBean : ClassTag](configKey: String)(implicit properties: PropertyResolver): Option[ScheduledJob[J, _ <: Trigger]] =
    if (properties.getProperty[JBoolean](s"$configKey.unscheduled", classOf[JBoolean], false)) {
      None
    } else if (properties.containsProperty(s"$configKey.cron")) {
      Some(CronTriggerJob[J](
        cronExpression = properties.getRequiredProperty(s"$configKey.cron"),
        name = properties.getProperty(s"$configKey.name", defaultJobName[J]),
        misfireInstruction = properties.getProperty[JInteger](s"$configKey.misfireInstruction", classOf[JInteger], CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING)
      ))
    } else if (properties.containsProperty(s"$configKey.repeat")) {
      Some(SimpleTriggerJob[J](
        repeatInterval = parseDuration(properties.getRequiredProperty(s"$configKey.repeat")),
        name = properties.getProperty(s"$configKey.name", defaultJobName[J]),
        misfireInstruction = properties.getProperty[JInteger](s"$configKey.misfireInstruction", classOf[JInteger], SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT)
      ))
    } else {
      None
    }

  /**
   * Configure one or more `JobConfiguration`s from a Spring `Environment` with the specified base `configKey`.
   *
   * This will first try to configure a single scheduled job at the specified key, and if no config exists
   * then it will then try and configure multiple scheduled jobs from an array of config keys under this one,
   * starting at 0. If that fails, it will return a single unscheduled job with no trigger information.
   *
   * @param configKey
   *         The config key to resolve for job configuration parameters
   * @param properties
   *         The property resolver to resolve parameters against
   * @tparam J
   *         The type of AutowiredJobBean to configure
   * @return
   *         A sequence of at least one JobConfiguration
   */
  def propertiesConfiguredJob[J <: AutowiredJobBean : ClassTag](configKey: String)(implicit properties: PropertyResolver): Seq[JobConfiguration[J]] =
    propertiesConfiguredScheduledJob[J](configKey).map(j => Seq(j))
      .orElse {
        Option {
          LazyList.from(0)
            .map(i => propertiesConfiguredScheduledJob[J](s"$configKey.$i"))
            .takeWhile(_.nonEmpty)
            .collect { case Some(j) => j }
        }.filterNot(_.isEmpty)
      }
      .getOrElse(Seq(SimpleUnscheduledJob[J](properties.getProperty(s"$configKey.name", defaultJobName[J]))))

  /**
    * Be very careful about changing the names of jobs here. If a job with a name is no longer referenced,
    * we will clear out ALL Quartz data the next time that the application starts, which may have unintended
    * side-effects (but will probably be fine). Adding new jobs is fine, it's just dereferencing or renaming
    * that's awkward.
    *
    * @see http://codrspace.com/Khovansa/spring-quartz-with-a-database/
    */
  def scheduledJobs(implicit properties: PropertyResolver): Seq[JobConfiguration[_ <: AutowiredJobBean]] = Seq(
    // Unscheduled jobs that are triggered explicitly by Sysadmin things
    propertiesConfiguredJob[ImportProfilesSingleDepartmentJob]("scheduling.importProfilesSingleDepartment"),
    propertiesConfiguredJob[ImportAssignmentsJob]("scheduling.importAssignments"),
    propertiesConfiguredJob[ImportAssignmentsAllYearsJob]("scheduling.importAssignmentsAllYears"),
    propertiesConfiguredJob[ImportSmallGroupEventLocationsJob]("scheduling.importSmallGroupEventLocations"),
    propertiesConfiguredJob[TurnitinTcaRegisterWebhooksJob]("scheduling.turnitinTcaRegisterWebhooks"),
    propertiesConfiguredJob[BulkImportModuleRegistrationsJob]("scheduling.bulkImportModuleRegistrations"),

    // Imports
    propertiesConfiguredJob[ImportAcademicDataJob]("scheduling.importAcademicData"),
    propertiesConfiguredJob[ImportProfilesJob]("scheduling.importProfiles"),
    propertiesConfiguredJob[StampMissingRowsJob]("scheduling.stampMissingRows"),
    propertiesConfiguredJob[ImportModuleMembershipDataJob]("scheduling.importModuleMembershipData"),
    propertiesConfiguredJob[ManualMembershipWarningJob]("scheduling.manualMembershipWarning"),
    propertiesConfiguredJob[ImportModuleListsJob]("scheduling.importModuleLists"),

    propertiesConfiguredJob[RemoveAgedApplicantsJob]("scheduling.removeAgedApplicants"),

    propertiesConfiguredJob[CleanupTemporaryFilesJob]("scheduling.cleanupTemporaryFiles"),

    propertiesConfiguredJob[ProcessScheduledNotificationsJob]("scheduling.processScheduledNotifications"),
    propertiesConfiguredJob[ProcessTriggersJob]("scheduling.processTriggers"),

    propertiesConfiguredJob[ProcessEmailQueueJob]("scheduling.processEmailQueue"),
    propertiesConfiguredJob[ProcessNotificationListenersJob]("scheduling.processNotificationListeners"),

    propertiesConfiguredJob[ProcessJobQueueJob]("scheduling.processJobQueue"),

    propertiesConfiguredJob[UpdateCheckpointTotalsJob]("scheduling.updateCheckpointTotals"),

    propertiesConfiguredJob[ProcessTurnitinLtiQueueJob]("scheduling.processTurnitinLtiQueue"),

    // Migration now complete, don't need this any more
    // propertiesConfiguredJob[ObjectStorageMigrationJob]("scheduling.objectStorageMigration"),
  ).flatten

  def scheduledSitsJobs(implicit properties: PropertyResolver): Seq[JobConfiguration[_ <: AutowiredJobBean]] = Seq(
    // SITS exports
    propertiesConfiguredJob[ExportAttendanceToSitsJob]("scheduling.exportAttendanceToSits"),
    propertiesConfiguredJob[ExportFeedbackToSitsJob]("scheduling.exportFeedbackToSits"),
    propertiesConfiguredJob[ExportYearMarksToSitsJob]("scheduling.exportYearMarksToSits")
  ).flatten
}

@Configuration
class JobFactoryConfiguration {
  @Bean def jobFactory(): AutowiringSpringBeanJobFactory = new AutowiringSpringBeanJobFactory
}

@Configuration
@Profile(Array("dev", "production", "scheduling", "sandbox"))
class SchedulingConfiguration {

  @Autowired var transactionManager: PlatformTransactionManager = _
  @Qualifier("dataSource")
  @Autowired var dataSource: DataSource = _
  @Autowired var jobFactory: SpringBeanJobFactory = _

  @Autowired var env: Environment = _
  @Autowired var propertiesPlaceholderConfigurer: PropertySourcesPlaceholderConfigurer = _
  lazy val properties: PropertySourcesPropertyResolver = new PropertySourcesPropertyResolver(propertiesPlaceholderConfigurer.getAppliedPropertySources)
  @Autowired var maintenanceModeService: MaintenanceModeService = _

  @Value("${toplevel.url}") var toplevelUrl: String = _

  private def scheduler(jobs: Seq[JobConfiguration[_ <: AutowiredJobBean]]): FactoryBean[Scheduler] = {
    // If we're deploying a change that means an existing trigger is no longer referenced, clear the scheduler
    val triggerNames: Seq[String] =
      new JdbcTemplate(dataSource).queryAndMap("select trigger_name from qrtz_triggers") {
        case (rs, _) => rs.getString("trigger_name")
      }

    val jobNames = jobs.map(_.name)

    // Clear the scheduler if there is a trigger that we no longer want to run
    val clearScheduler = !triggerNames.forall(jobNames.contains)

    val factory = new SchedulerFactoryBean() {
      override def createScheduler(schedulerFactory: SchedulerFactory, schedulerName: String): Scheduler = {
        val scheduler = super.createScheduler(schedulerFactory, schedulerName)

        if (clearScheduler) {
          scheduler.clear()
        }

        scheduler
      }
    }

    factory.setConfigLocation(new ClassPathResource("/quartz.properties"))
    factory.setStartupDelay(10)
    factory.setDataSource(dataSource)
    factory.setTransactionManager(transactionManager)
    factory.setSchedulerName(Uri.parse(toplevelUrl).getAuthority)
    factory.setOverwriteExistingJobs(true)

    // We only auto-startup on the scheduler, and only if we're not in maintenance mode. This allows us
    // to wire a scheduler on nodes that wouldn't normally get one and use it to schedule jobs. Neat!
    factory.setAutoStartup(env.acceptsProfiles(Profiles.of("scheduling")) && !maintenanceModeService.enabled)

    if (!env.acceptsProfiles(Profiles.of("scheduling"))) {
      factory.setQuartzProperties(new Properties() {
        {
          setProperty("org.quartz.jobStore.isClustered", "false")
        }
      })
    }

    factory.setApplicationContextSchedulerContextKey("applicationContext")
    factory.setJobFactory(jobFactory)

    factory.setJobDetails(jobs.map(_.jobDetail).distinct: _*)
    factory.setTriggers(jobs.collect { case j: ScheduledJob[_, _] => j.trigger }: _*)

    factory
  }

  @Bean
  @Profile(Array("dev", "production"))
  def schedulerWithSitsExports(): FactoryBean[Scheduler] = {
    scheduler(SchedulingConfiguration.scheduledJobs(properties) ++ SchedulingConfiguration.scheduledSitsJobs(properties))
  }

  @Bean
  @Profile(Array("sandbox"))
  def schedulerNoSitsExports(): FactoryBean[Scheduler] = {
    scheduler(SchedulingConfiguration.scheduledJobs(properties))
  }

}

@Configuration
@Profile(Array("test"))
class TestSchedulingConfiguration {
  @Bean def scheduler(): FactoryBean[Scheduler] = {
    new SchedulerFactoryBean
  }
}

@Component
@Profile(Array("scheduling"))
class SchedulingMaintenanceModeObserver extends InitializingBean {

  @Autowired var maintenanceModeService: MaintenanceModeService = _
  @Autowired var scheduler: Scheduler = _

  override def afterPropertiesSet(): Unit = {
    // listen for maintenance mode changes
    maintenanceModeService.changingState.observe { enabled =>
      if (enabled) scheduler.standby()
      else scheduler.start()
    }
  }
}

@Component
@Profile(Array("scheduling"))
class SchedulingTurnitinTcaRegisterWebhooks extends InitializingBean with Logging {

  @Autowired var scheduler: Scheduler = _

  override def afterPropertiesSet(): Unit = {
    // TODO - it would be better if we only ran this once but I can't figure out a way of nominating a single scheduler node.
    // This will be run once per scheduler node but the Job has DisallowConcurrentExecution so that shouldn't cause any issues
    scheduler.scheduleNow[TurnitinTcaRegisterWebhooksJob]()
  }
}

trait AutowiredJobBean extends QuartzJobBean {

  protected var features: Features = Wire[Features]
  protected var exceptionResolver: ExceptionResolver = Wire[ExceptionResolver]

}
