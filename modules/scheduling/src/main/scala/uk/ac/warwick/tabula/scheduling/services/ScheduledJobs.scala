package uk.ac.warwick.tabula.scheduling.services

import org.joda.time.DateTime
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.scheduling.commands._
import uk.ac.warwick.tabula.scheduling.commands.imports._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.elasticsearch.{NotificationIndexService, AuditEventIndexService, ProfileIndexService}
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.tabula.system.exceptions.ExceptionResolver
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.{AcademicYear, Features}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * The scheduled jobs don't particularly have to all be in one class,
 * but I decided it's better to have them all together than it is to have
 * the scheduled method in a related class (since it does so little) - nick
 */
@Service
class ScheduledJobs {

	var fileSyncEnabled = Wire[JBoolean]("${environment.standby:false}")
	var features = Wire[Features]

	var exceptionResolver = Wire[ExceptionResolver]
	var maintenanceModeService = Wire[MaintenanceModeService]

	var auditIndexingService = Wire[AuditEventIndexService]
	var profileIndexingService = Wire[ProfileIndexService]
	var jobService = Wire[JobService]
	var notificationIndexService = Wire[NotificationIndexService]
	var notificationEmailService = Wire[EmailNotificationService]
	var scheduledNotificationService = Wire[ScheduledNotificationService]
	implicit var termService = Wire[TermService]
	var triggerService = Wire[TriggerService]

	def maintenanceGuard[A](fn: => A) = if (!maintenanceModeService.enabled) fn

	def syncGuard[A](fn: => A) = if (fileSyncEnabled) fn

	@Scheduled(cron = "0 0 7,14 * * *")
	def importData(): Unit =
		if (features.schedulingAcademicInformationImport) maintenanceGuard {
			exceptionResolver.reportExceptions {
				ImportAcademicInformationCommand().apply()
			}
		}

	@Scheduled(cron = "0 30 0 * * *")
	def importMembers(): Unit =
		if (features.schedulingProfilesImport) maintenanceGuard {
			exceptionResolver.reportExceptions {
				new ImportProfilesCommand().apply()
			}
		}

	@Scheduled(cron = "0 0 7 * * *")
	def importAssignments(): Unit =
		if (features.schedulingAssignmentsImport) maintenanceGuard {
			exceptionResolver.reportExceptions {
				ImportAssignmentsCommand().apply()
			}
		}

	@Scheduled(cron = "0 0 2 * * *") // 2am
	def cleanupTemporaryFiles(): Unit =
		if (features.schedulingCleanupTemporaryFiles) maintenanceGuard {
			exceptionResolver.reportExceptions {
				new CleanupTemporaryFilesCommand().apply()
			}
		}

	@Scheduled(fixedRate = 60 * 1000) // every minute
	def indexAuditEvents(): Unit =
		if (features.schedulingAuditIndex)
			exceptionResolver.reportExceptions { Await.result(auditIndexingService.incrementalIndex(), Duration.Inf) }

	@Scheduled(cron = "0 0-59/5 3-23 * * *") // every 5 minutes, except between midnight and 3am (when the member import happens)
	def indexProfiles(): Unit =
		if (features.schedulingProfilesIndex)
			exceptionResolver.reportExceptions { Await.result(profileIndexingService.incrementalIndex(), Duration.Inf) }

	@Scheduled(fixedRate = 60 * 1000) // every minute
	def indexNotifications(): Unit =
		if (features.schedulingNotificationsIndex)
			exceptionResolver.reportExceptions { Await.result(notificationIndexService.incrementalIndex(), Duration.Inf) }

	@Scheduled(fixedRate = 60 * 1000) // every minute
	def resolveScheduledNotifications(): Unit =
		if (features.schedulingProcessScheduledNotifications) maintenanceGuard {
			exceptionResolver.reportExceptions {
				scheduledNotificationService.processNotifications()
			}
		}

	@Scheduled(fixedDelay = 10 * 1000) // every 10 seconds, non-concurrent
	def processTriggers(): Unit =
		if (features.schedulingTriggers) maintenanceGuard {
			exceptionResolver.reportExceptions {
				triggerService.processTriggers()
			}
		}

	@Scheduled(fixedRate = 60 * 1000) // every minute
	def processEmailQueue(): Unit =
		if (features.schedulingNotificationEmails) maintenanceGuard {
			exceptionResolver.reportExceptions {
				notificationEmailService.processNotifications()
			}
		}

	@Scheduled(fixedRate = 10 * 1000) // every 10 seconds
	def jobs(): Unit =
		if (features.schedulingJobService) maintenanceGuard {
			exceptionResolver.reportExceptions { jobService.run() }
		}

	@Scheduled(fixedDelay = 5 * 60 * 1000) // every 5 minutes, non-concurrent
	def exportAttendanceToSits(): Unit =
		if (features.schedulingExportAttendanceToSits) maintenanceGuard {
			exceptionResolver.reportExceptions { ExportAttendanceToSitsCommand().apply() }
		}

	@Scheduled(cron = "0 0 4 * * *") // 4am
	def updateAttendanceMonitoringSchemeMembership(): Unit =
		if (features.schedulingAttendanceUpdateSchemes) maintenanceGuard {
			exceptionResolver.reportExceptions {
				UpdateAttendanceMonitoringSchemeMembershipCommand().apply()
			}
			exceptionResolver.reportExceptions {
				val thisAcademicYear = AcademicYear.findAcademicYearContainingDate(DateTime.now)
				if (thisAcademicYear.isSITSInFlux(DateTime.now)) {
					UnlinkAttendanceMonitoringSchemeCommand().apply()
				}
			}
		}

	@Scheduled(fixedDelay = 5 * 60 * 1000) // every 5 minutes, non-concurrent
	def exportFeedbackToSits(): Unit =
		if (features.schedulingExportFeedbackToSits) maintenanceGuard {
			exceptionResolver.reportExceptions { ExportFeedbackToSitsCommand().apply() }
		}

	@Scheduled(fixedDelay = 1 * 60 * 1000) // every minute, non-concurrent
	def objectStorageMigration(): Unit =
		// We don't really need a maintenance guard here, but it stops it running on the standby
		if (features.schedulingObjectStorageMigration) maintenanceGuard {
			exceptionResolver.reportExceptions { ObjectStorageMigrationCommand().apply() }
		}

}
