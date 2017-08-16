package uk.ac.warwick.tabula.services.scheduling.jobs

import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.scheduling.imports._
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.util.Try

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class ImportSmallGroupEventLocationsJob extends AutowiredJobBean {

	override def executeInternal(context: JobExecutionContext): Unit = {

		exceptionResolver.reportExceptions {
			context.getMergedJobDataMap.getString("academicYear").maybeText.flatMap(s => Try(s.toInt).toOption).foreach(year => {
				val cmd = ImportSmallGroupEventLocationsCommand(AcademicYear(year))
				cmd.apply()
			})
		}

	}

}
