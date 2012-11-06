package uk.ac.warwick.courses.services

import java.util.Collections.newSetFromMap
import java.util.concurrent.ConcurrentHashMap
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import uk.ac.warwick.courses._
import uk.ac.warwick.courses.commands.CleanupTemporaryFilesCommand
import uk.ac.warwick.courses.commands.imports.ImportModulesCommand
import uk.ac.warwick.courses.services.jobs.JobService
import uk.ac.warwick.courses.system.exceptions.ExceptionResolver
import uk.ac.warwick.courses.commands.imports.ImportAssignmentsCommand

/**
 * The scheduled jobs don't particularly have to all be in one class,
 * but I decided it's better to have them all together than it is to have
 * the scheduled method in a related class (since it does so little) - nick
 */
@Service
class ScheduledJobs {
	import SchedulingConcurrency._

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

	@Scheduled(cron = "0 */1 * * * *") // every minute
	def indexAuditEvents: Unit = exceptionResolver.reportExceptions { indexingService.index }

	@Scheduled(cron = "*/10 * * * * *") // every 10 seconds
	def jobs: Unit = nonconcurrent("scheduled-jobs") {
		exceptionResolver.reportExceptions {
			jobService.run
		}
	}
}

/**
 * Provides `nonconcurrent` which will only run the given
 * code if it's not currently running (as determined by the
 * ID string also passed). Used to avoid running a task if it's
 * still running already.
 */
object SchedulingConcurrency {

	var running = newSetFromMap(new ConcurrentHashMap[String, JBoolean])
	def nonconcurrent[T](id: String)(f: => T) =
		if (running.add(id)) {
			try f
			finally running.remove(id)
		}
}