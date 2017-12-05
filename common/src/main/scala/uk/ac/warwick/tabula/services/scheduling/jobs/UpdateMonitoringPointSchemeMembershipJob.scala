package uk.ac.warwick.tabula.services.scheduling.jobs

import org.joda.time.LocalDate
import org.quartz.{DisallowConcurrentExecution, JobExecutionContext}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.{Profile, Scope}
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.scheduling.{UnlinkAttendanceMonitoringSchemeCommand, UpdateAttendanceMonitoringSchemeMembershipCommand}
import uk.ac.warwick.tabula.services.scheduling.AutowiredJobBean

@Component
@Profile(Array("scheduling"))
@DisallowConcurrentExecution
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
class UpdateMonitoringPointSchemeMembershipJob extends AutowiredJobBean {

	override def executeInternal(context: JobExecutionContext): Unit = {
		if (features.schedulingAttendanceUpdateSchemes) {
			exceptionResolver.reportExceptions {
				UpdateAttendanceMonitoringSchemeMembershipCommand().apply()
			}
			exceptionResolver.reportExceptions {
				val thisAcademicYear = AcademicYear.now()
				if (thisAcademicYear.isSITSInFlux(LocalDate.now)) {
					UnlinkAttendanceMonitoringSchemeCommand().apply()
				}
			}
		}
	}

}