package uk.ac.warwick.tabula.scheduling.scheduler

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.scheduling.ExportFeedbackToSitsCommand

@Component
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ExportFeedbackToSitsJob extends AutowiredJobBean {

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingExportFeedbackToSits) maintenanceGuard {
			exceptionResolver.reportExceptions { ExportFeedbackToSitsCommand().apply() }
		}
	}

}