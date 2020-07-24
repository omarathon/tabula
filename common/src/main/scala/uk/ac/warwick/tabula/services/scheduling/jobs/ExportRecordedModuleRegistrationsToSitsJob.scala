package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.EarlyRequestInfo
import uk.ac.warwick.tabula.commands.scheduling.ExportRecordedModuleRegistrationsToSitsCommand
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ExportRecordedModuleRegistrationsToSitsJob extends AutowiredJobBean {

  override def executeInternal(context: JobExecutionContext): Unit = {
    //Used separate feaure flag for this so we can continue deploying on live. A couple of questions awaiting confirmation from exams
    if (features.schedulingExportRecordedModuleMarksToSits)
      exceptionResolver.reportExceptions {
        EarlyRequestInfo.wrap() {
          ExportRecordedModuleRegistrationsToSitsCommand().apply()
        }
      }
  }

}
