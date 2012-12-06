package uk.ac.warwick.tabula.scheduling.services

import uk.ac.warwick.tabula.services._
import java.util.Collections.newSetFromMap
import java.util.concurrent.ConcurrentHashMap
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.CleanupTemporaryFilesCommand
import uk.ac.warwick.tabula.commands.imports.ImportModulesCommand
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.tabula.system.exceptions.ExceptionResolver
import uk.ac.warwick.tabula.commands.imports.ImportAssignmentsCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import scala.reflect.BeanProperty

/**
 * The scheduled jobs don't particularly have to all be in one class,
 * but I decided it's better to have them all together than it is to have
 * the scheduled method in a related class (since it does so little) - nick
 */
@Service
class ScheduledJobs {
	@Autowired @BeanProperty
	var exceptionResolver: ExceptionResolver = _

	@Autowired @BeanProperty
	var indexingService: AuditEventIndexService = _

	@Autowired @BeanProperty
	var jobService: JobService = _

	@Scheduled(cron = "0 0 7,14 * * *")
	def importData: Unit = exceptionResolver.reportExceptions {
		new ImportModulesCommand().apply()
	}

	@Scheduled(cron = "0 30 8 * * *")
	def importAssignments: Unit = exceptionResolver.reportExceptions {
		new ImportAssignmentsCommand().apply()
	}

	@Scheduled(cron = "0 0 2 * * *") // 2am
	def cleanupTemporaryFiles: Unit = exceptionResolver.reportExceptions {
		new CleanupTemporaryFilesCommand().apply()
	}

	@Scheduled(fixedRate = 60000) // every minute
	def indexAuditEvents: Unit = exceptionResolver.reportExceptions { indexingService.index }

	@Scheduled(fixedDelay = 10000) // every 10 seconds, non-concurrent
	def jobs: Unit = exceptionResolver.reportExceptions { jobService.run }

}