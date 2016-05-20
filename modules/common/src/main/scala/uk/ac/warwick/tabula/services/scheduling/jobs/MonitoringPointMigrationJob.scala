package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.scheduling.MonitoringPointMigrationCommand
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AutowiringMonitoringPointServiceComponent
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class MonitoringPointMigrationJob extends AutowiredJobBean with AutowiringMonitoringPointServiceComponent with Logging {

	override def executeInternal(context: JobExecutionContext): Unit = {
		// We don't really need a maintenance guard here, but it stops it running on the standby
		if (features.schedulingMonitoringPointMigration)
			exceptionResolver.reportExceptions {
				transactional() {
					monitoringPointService.getSetToMigrate match {
						case Some(set) => MonitoringPointMigrationCommand(set).apply()
						case _ => logger.warn("Could not find any monitoring point sets to migrate; consider disabling the job")
					}
				}
			}
	}

}