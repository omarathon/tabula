package uk.ac.warwick.tabula.services.scheduling.jobs

import org.joda.time.LocalDate
import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.scheduling.{UnlinkSmallGroupSetCommand, UpdateLinkedSmallGroupSetsCommand}
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean
import uk.ac.warwick.tabula.system.exceptions.ExceptionResolver
import uk.ac.warwick.tabula.{AcademicYear, EarlyRequestInfo, Features}

object UpdateLinkedSmallGroupSetJob {
  def execute(features: Features, exceptionResolver: ExceptionResolver): Unit =
    if (features.schedulingGroupsUpdateDepartmentSets) {
      exceptionResolver.reportExceptions {
        EarlyRequestInfo.wrap() {
          UpdateLinkedSmallGroupSetsCommand().apply()
        }
      }

      exceptionResolver.reportExceptions {
        EarlyRequestInfo.wrap() {
          val thisAcademicYear = AcademicYear.now()
          if (thisAcademicYear.isSITSInFlux(LocalDate.now)) {
            UnlinkSmallGroupSetCommand().apply()
          }
        }
      }
    }
}

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class UpdateLinkedSmallGroupSetJob extends AutowiredJobBean {

  override def executeInternal(context: JobExecutionContext): Unit =
    UpdateLinkedSmallGroupSetJob.execute(features, exceptionResolver)

}
