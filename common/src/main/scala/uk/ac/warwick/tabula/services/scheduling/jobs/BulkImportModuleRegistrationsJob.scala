package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.scheduling.imports.BulkImportModuleRegistrationsCommand
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.util.Try

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class BulkImportModuleRegistrationsJob extends AutowiredJobBean with Logging {

  override def executeInternal(context: JobExecutionContext): Unit = {
    exceptionResolver.reportExceptions {
      val academicYear = Option(context.getMergedJobDataMap.getString("academicYear"))
        .flatMap(_.maybeText)
        .flatMap(s => Try(s.toInt).toOption)
        .map(AcademicYear.apply)

      academicYear.foreach(BulkImportModuleRegistrationsCommand(_).apply())
    }
  }

}