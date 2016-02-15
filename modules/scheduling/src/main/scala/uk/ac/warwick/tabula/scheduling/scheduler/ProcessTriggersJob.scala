package uk.ac.warwick.tabula.scheduling.scheduler

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.services.TriggerService

@Component
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ProcessTriggersJob extends AutowiredJobBean {

	@Autowired var triggerService: TriggerService = _

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingTriggers) maintenanceGuard {
			exceptionResolver.reportExceptions {
				triggerService.processTriggers()
			}
		}
	}

}
