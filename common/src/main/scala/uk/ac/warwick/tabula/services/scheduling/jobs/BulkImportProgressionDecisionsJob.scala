package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.scheduling.imports.BulkImportProgressionDecisionsCommand
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean
import uk.ac.warwick.tabula.system.exceptions.ExceptionResolver
import uk.ac.warwick.tabula.{AcademicYear, EarlyRequestInfo, Features}

import scala.util.Try

object BulkImportProgressionDecisionsJob {
  def execute(features: Features, exceptionResolver: ExceptionResolver)(academicYear: Option[AcademicYear]): Unit =
    if (features.schedulingBulkProgressionDecisionsImport)
      exceptionResolver.reportExceptions {
        EarlyRequestInfo.wrap() {
          BulkImportProgressionDecisionsCommand(academicYear.getOrElse(AcademicYear.now())).apply()
        }
      }
}

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class BulkImportProgressionDecisionsJob extends AutowiredJobBean with Logging {

  override def executeInternal(context: JobExecutionContext): Unit =
    BulkImportProgressionDecisionsJob.execute(features, exceptionResolver) {
      Option(context.getMergedJobDataMap.getString("academicYear"))
        .flatMap(_.maybeText)
        .flatMap(s => Try(s.toInt).toOption)
        .map(AcademicYear.apply)
    }

}
