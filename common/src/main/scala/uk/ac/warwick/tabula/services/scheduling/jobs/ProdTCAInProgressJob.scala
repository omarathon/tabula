package uk.ac.warwick.tabula.services.scheduling.jobs

import org.joda.time.LocalDate
import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.DateFormats.DatePickerFormatter
import uk.ac.warwick.tabula.EarlyRequestInfo
import uk.ac.warwick.tabula.commands.scheduling.ProdTCAInProgressCommand
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ProdTCAInProgressJob extends AutowiredJobBean with Logging {

  override def executeInternal(context: JobExecutionContext): Unit = {
    if (features.turnitinTca)
        EarlyRequestInfo.wrap() {
          val since = Option(context.getMergedJobDataMap.getString("since")).flatMap(_.maybeText)
          val command = ProdTCAInProgressCommand()
          command.since = LocalDate.parse(since.get, DatePickerFormatter)
          command.apply()
        }
  }
}
