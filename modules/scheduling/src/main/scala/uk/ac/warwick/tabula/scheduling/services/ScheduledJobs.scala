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
import uk.ac.warwick.tabula.commands.imports.ImportProfilesCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import scala.reflect.BeanProperty
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.scheduling.commands.SyncReplicaFilesystemCommand
import uk.ac.warwick.tabula.scheduling.commands.SyncReplicaFilesystemCommand

/**
 * The scheduled jobs don't particularly have to all be in one class,
 * but I decided it's better to have them all together than it is to have
 * the scheduled method in a related class (since it does so little) - nick
 */
@Service
class ScheduledJobs {
	
	var fileSyncEnabled = Wire.property("${environment.standby}").toBoolean

	var exceptionResolver = Wire.auto[ExceptionResolver]
	
	var maintenanceModeService = Wire.auto[MaintenanceModeService]

	var auditIndexingService = Wire.auto[AuditEventIndexService]
	
	var profileIndexingService = Wire.auto[ProfileIndexService]

	var jobService = Wire.auto[JobService]
	
	def maintenanceGuard[T](fn: => T) = if (!maintenanceModeService.enabled) fn
	
	def syncGuard[T](fn: => T) = if (fileSyncEnabled) fn

	@Scheduled(cron = "0 0 7,14 * * *")
	def importData: Unit = maintenanceGuard {
		exceptionResolver.reportExceptions {
			new ImportModulesCommand().apply()
		}
	}

	@Scheduled(cron = "0 30 7 * * *")
	def importMembers: Unit = maintenanceGuard {
		exceptionResolver.reportExceptions {
			new ImportProfilesCommand().apply()
		}
	}

	@Scheduled(cron = "0 30 8 * * *")
	def importAssignments: Unit = maintenanceGuard {
		exceptionResolver.reportExceptions {
			new ImportAssignmentsCommand().apply()
		}
	}

	@Scheduled(cron = "0 0 2 * * *") // 2am
	def cleanupTemporaryFiles: Unit = maintenanceGuard {
		exceptionResolver.reportExceptions {
			new CleanupTemporaryFilesCommand().apply()
		}
	}

	@Scheduled(fixedRate = 60 * 1000) // every minute
	def indexAuditEvents: Unit = exceptionResolver.reportExceptions { auditIndexingService.index }
	
	@Scheduled(fixedRate = 300 * 1000) // every 5 minutes
	def indexProfiles: Unit = exceptionResolver.reportExceptions { profileIndexingService.index }

	@Scheduled(fixedDelay = 10 * 1000) // every 10 seconds, non-concurrent
	def jobs: Unit = maintenanceGuard {
		exceptionResolver.reportExceptions { jobService.run }
	}
	
	/* Filesystem syncing jobs, should only run on standby */
	@Scheduled(fixedRate = 300 * 1000) // every 5 minutes
	def fileSync: Unit = syncGuard {
		exceptionResolver.reportExceptions {
			new SyncReplicaFilesystemCommand().apply()
		}
	}

}
