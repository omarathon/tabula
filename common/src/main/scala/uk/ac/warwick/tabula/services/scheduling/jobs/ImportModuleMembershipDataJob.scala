package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ImportModuleMembershipDataJob extends AutowiredJobBean {

  override def executeInternal(context: JobExecutionContext): Unit =
    if (features.schedulingCombinedModuleMembershipDataImport) {
      // Assessment components & assessment group membership
      ImportAssignmentsJob.execute(features, exceptionResolver)

      // Get module registrations in bulk for the current a/y
      BulkImportModuleRegistrationsJob.execute(features, exceptionResolver)(Some(AcademicYear.now()))

      // Small group sets linked to SITS
      UpdateLinkedSmallGroupSetJob.execute(features, exceptionResolver)

      // Department small group sets linked to SITS
      UpdateLinkedDepartmentSmallGroupSetJob.execute(features, exceptionResolver)

      // Monitoring point schemes linked to SITS
      UpdateMonitoringPointSchemeMembershipJob.execute(features, exceptionResolver)
    }
}
