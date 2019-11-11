package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.{EarlyRequestInfo, Features}
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportAssignmentsCommand
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean
import uk.ac.warwick.tabula.system.exceptions.ExceptionResolver

object ImportAssignmentsJob {
  def execute(features: Features, exceptionResolver: ExceptionResolver): Unit =
    if (features.schedulingAssignmentsImport)
      exceptionResolver.reportExceptions {
        EarlyRequestInfo.wrap() {
          ImportAssignmentsCommand().apply()
        }
      }
}

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ImportAssignmentsJob extends AutowiredJobBean {

  override def executeInternal(context: JobExecutionContext): Unit =
    ImportAssignmentsJob.execute(features, exceptionResolver)

}
