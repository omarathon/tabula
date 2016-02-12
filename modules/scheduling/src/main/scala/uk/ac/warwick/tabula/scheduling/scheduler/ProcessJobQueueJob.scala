package uk.ac.warwick.tabula.scheduling.scheduler

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.services.jobs.JobService

@Component
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ProcessJobQueueJob extends AutowiredJobBean {

	@Autowired var jobService: JobService = _

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingJobService) maintenanceGuard {
			exceptionResolver.reportExceptions { jobService.run() }
		}
	}

}
