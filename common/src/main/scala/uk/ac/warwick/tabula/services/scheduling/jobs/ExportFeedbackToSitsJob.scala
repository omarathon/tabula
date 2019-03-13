package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.EarlyRequestInfo
import uk.ac.warwick.tabula.commands.scheduling.ExportFeedbackToSitsCommand
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ExportFeedbackToSitsJob extends AutowiredJobBean {

  override def executeInternal(context: JobExecutionContext): Unit = {
    if (features.schedulingExportFeedbackToSits)
      exceptionResolver.reportExceptions {
        EarlyRequestInfo.wrap() {
          ExportFeedbackToSitsCommand().apply()
        }
      }
  }

}