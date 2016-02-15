package uk.ac.warwick.tabula.scheduling.scheduler

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.scheduling.ObjectStorageMigrationCommand

@Component
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ObjectStorageMigrationJob extends AutowiredJobBean {

	override def executeInternal(context: JobExecutionContext): Unit = {
		// We don't really need a maintenance guard here, but it stops it running on the standby
		if (features.schedulingObjectStorageMigration) maintenanceGuard {
			exceptionResolver.reportExceptions { ObjectStorageMigrationCommand().apply() }
		}
	}

}