package uk.ac.warwick.tabula.scheduling.scheduler

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.services.EmailNotificationService

@Component
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ProcessEmailQueueJob extends AutowiredJobBean {

	@Autowired var emailNotificationService: EmailNotificationService = _

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingNotificationEmails) maintenanceGuard {
			exceptionResolver.reportExceptions {
				emailNotificationService.processNotifications()
			}
		}
	}

}
