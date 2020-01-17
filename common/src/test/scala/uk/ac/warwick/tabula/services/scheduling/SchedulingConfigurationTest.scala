package uk.ac.warwick.tabula.services.scheduling

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.env.PropertySourcesPropertyResolver
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.services.scheduling.SchedulingConfiguration.{CronTriggerJob, JobConfiguration, SimpleTriggerJob, SimpleUnscheduledJob}
import uk.ac.warwick.tabula.services.scheduling.jobs.{ImportModuleMembershipDataJob, ImportProfilesSingleDepartmentJob, ManualMembershipWarningJob}

import scala.concurrent.duration._

class SchedulingConfigurationTest extends AppContextTestBase {

  @Autowired var propertiesPlaceholderConfigurer: PropertySourcesPlaceholderConfigurer = _
  lazy val properties: PropertySourcesPropertyResolver = new PropertySourcesPropertyResolver(propertiesPlaceholderConfigurer.getAppliedPropertySources)

  @Test def itWorks(): Unit = {
    val jobs: Seq[JobConfiguration[_ <: AutowiredJobBean]] =
      SchedulingConfiguration.scheduledJobs(properties) ++ SchedulingConfiguration.scheduledSitsJobs(properties)

    jobs should not be Symbol("empty")

    // Check a cron job, a simple trigger job, a multi-config job and an unscheduled job
    val cleanupTemporaryFilesJob =
      jobs.filter(_.name == "CleanupTemporaryFilesJob").collectFirst { case c: CronTriggerJob[_] => c }
        .getOrElse(fail("Couldn't find a CleanupTemporaryFilesJob cron trigger"))

    cleanupTemporaryFilesJob.cronExpression should be("0 0 10 ? * SUN *")

    val processScheduledNotificationsJob =
      jobs.filter(_.name == "ProcessScheduledNotificationsJob").collectFirst { case s: SimpleTriggerJob[_] => s }
        .getOrElse(fail("Couldn't find a ProcessScheduledNotificationsJob simple trigger"))

    processScheduledNotificationsJob.repeatInterval should be (20.seconds)

    val manualMembershipWarningJobs =
      jobs.filter(_.name == "ManualMembershipWarningJob").collect { case c: CronTriggerJob[ManualMembershipWarningJob @unchecked] => c }

    manualMembershipWarningJobs.size should be (2)
    manualMembershipWarningJobs.map(_.cronExpression).sorted should be (Seq("0 0 9 ? 1/1 MON#1 *", "0 0 9 ? 1/1 MON#3 *"))

    val importProfilesSingleDepartmentJob =
      jobs.filter(_.name == "ImportProfilesSingleDepartmentJob").collectFirst { case j: SimpleUnscheduledJob[ImportProfilesSingleDepartmentJob @unchecked] => j }

    importProfilesSingleDepartmentJob should not be Symbol("empty")

    // This job is actively unscheduled in test/resources/tabula.properties
    val importModuleMembershipDataJob =
      jobs.filter(_.name == "ImportModuleMembershipDataJob").collectFirst { case j: SimpleUnscheduledJob[ImportModuleMembershipDataJob @unchecked] => j }

    importProfilesSingleDepartmentJob should not be Symbol("empty")
  }

}
