package uk.ac.warwick.tabula.commands.scheduling.turnitin

import org.joda.time.DateTime
import org.springframework.transaction.annotation.Propagation._
import uk.ac.warwick.tabula.commands.scheduling.turnitin.ProcessTurnitinLtiBackoffQueueCommand.{Result, _}
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Notifies, Unaudited}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.notifications.coursework.{TurnitinJobErrorNotification, TurnitinJobSuccessNotification}
import uk.ac.warwick.tabula.data.model.{Assignment, Notification, OriginalityReport, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.turnitinlti._
import uk.ac.warwick.tabula.services.{AutowiringOriginalityReportServiceComponent, _}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{AutowiringTopLevelUrlComponent, CurrentUser, TopLevelUrlComponent}
import uk.ac.warwick.userlookup.User

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object ProcessTurnitinLtiBackoffQueueCommand {
	val AttemptInterval: FiniteDuration = 1.minute
	val MaxAssignmentAttempts: Int = 10
	val MaxSubmitAttempts: Int = 10
	val MaxResponseAttempts: Int = 10

	case class Result(completed: Seq[Assignment], failed: Seq[Assignment])

	def apply() = new ProcessTurnitinLtiBackoffQueueCommandInternal
		with ComposableCommand[Result]
		with AutowiringTurnitinLtiBackoffQueueServiceComponent
		with AutowiringTurnitinLtiServiceComponent
		with AutowiringAssessmentServiceComponent
		with AutowiringFileAttachmentServiceComponent
		with AutowiringTopLevelUrlComponent
		with AutowiringOriginalityReportServiceComponent
		with ProcessTurnitinLtiBackoffQueuePermissions
		with ProcessTurnitinLtiBackoffQueueNotification
		with Unaudited
}

abstract class ProcessTurnitinLtiBackoffQueueCommandInternal extends CommandInternal[Result] with Logging {
	self: TurnitinLtiBackoffQueueServiceComponent with TurnitinLtiServiceComponent with AssessmentServiceComponent with FileAttachmentServiceComponent with TopLevelUrlComponent with OriginalityReportServiceComponent =>

	private def queue: TurnitinLtiBackoffQueueService = turnitinLtiBackoffQueueService

	override def applyInternal(): ProcessTurnitinLtiBackoffQueueCommand.Result = {
		logger.info("Processing the Turnitin queue")

		val action = nextAction

		action.foreach(_ => logger.info("Found an action"))

		action.foreach(_.apply())

		finish()
	}

	private def nextAction: Option[() => Unit] =
		queue.nextAssignment.map(a => () => createAssignment(a))
			.orElse(queue.nextReportToSubmit.map(r => () => submitPaper(r)))
			.orElse(queue.nextReportToFetch.map(r => () => fetchReport(r)))

	private def userToSubmit(assignment: Assignment): User = {
		val module = assignment.module

		(assignment.turnitinLtiNotifyUsers ++ module.managers.users ++ module.adminDepartment.owners.users).head
	}

	private def createAssignment(assignment: Assignment): Unit = {
		logger.info(s"Processing assignment ${assignment.id}...")

		val user = userToSubmit(assignment)

		assignment.lastSubmittedToTurnitin = DateTime.now
		val response = turnitinLtiService.submitAssignment(assignment, new CurrentUser(user, user))

		if (response.success) {
			assignment.nextTurnitinSubmissionAttempt = null
			logger.info("Assignment was created in Turnitin successfully")
		} else {
			assignment.submitToTurnitinRetries += 1
			assignment.nextTurnitinSubmissionAttempt = nextAttempt(assignment).orNull
			logger.info(s"Next attempt will be at ${assignment.nextTurnitinSubmissionAttempt}")
		}

		assessmentService.save(assignment)

		if (assignment.nextTurnitinSubmissionAttempt == null) {
			logger.warn(s"Max retries reached for assignment ${assignment.id}, last response from Turnitin: ${response.html.getOrElse("No HTML response")}")
		}
	}

	private def submitPaper(report: OriginalityReport): Unit = {
		logger.info(s"Processing report for submission ${report.id}...")

		val attachment = report.attachment
		val submission = attachment.submissionValue.submission
		val assignment = submission.assignment

		val token = createAttachmentToken(attachment)

		val attachmentUrl = s"$toplevelUrl${Routes.turnitinlti.fileByToken(submission, attachment, token)}"

		val response = turnitinLtiService.submitPaper(
			assignment = assignment,
			paperUrl = attachmentUrl,
			userId = submission.usercode,
			userEmail = s"${submission.usercode}@TurnitinLti.warwick.ac.uk",
			attachment = attachment,
			userFirstName = submission.studentIdentifier,
			userLastName = "Student"
		)

		originalityReportService.refresh(report)

		report.submitAttempts += 1
		report.lastSubmittedToTurnitin = DateTime.now

		if (response.success) {
			report.turnitinId = response.turnitinSubmissionId()
			report.nextSubmitAttempt = null
			report.submittedDate = DateTime.now()
			logger.info("The paper was submitted")
		} else {
			report.lastTurnitinError = response.statusMessage.getOrElse("Failed to upload")
			logger.warn(s"Failed to submit paper for report ${report.id}: ${report.lastTurnitinError}")
			report.nextSubmitAttempt = nextSubmitAttempt(report).orNull
			logger.info(s"Next attempt will be at ${report.nextSubmitAttempt}")
		}

		originalityReportService.saveOrUpdate(report)
	}

	private def createAttachmentToken(attachment: FileAttachment) = {
		// Check for an existing valid token before creating a new one
		fileAttachmentService.getValidToken(attachment).getOrElse {
			// New transaction as Turnitin requests the file while keeping the submitPaper request pending
			transactional(propagation = REQUIRES_NEW) {
				val t = attachment.generateToken()
				fileAttachmentService.saveOrUpdate(t)
				t
			}
		}
	}

	private def fetchReport(report: OriginalityReport): Unit = {
		logger.info(s"Processing report for report ${report.id}...")
		report.lastReportRequest = DateTime.now

		val attachment = report.attachment
		val assignment = attachment.submissionValue.submission.assignment
		val user = userToSubmit(assignment)

		report.responseAttempts += 1

		turnitinLtiService.getSubmissionDetails(report.turnitinId, new CurrentUser(user, user)) match {
			case response if response.success && response.submissionInfo().similarity.isDefined =>
				saveReport(report, response.submissionInfo())
				report.reportReceived = true
				report.nextResponseAttempt = null
				report.responseReceived = DateTime.now()
				logger.info("Received the originality report")
			case response =>
				logger.info("Bad response for submission details  - " + response.json.orElse(response.html).orElse(response.xml).getOrElse(""))
				report.lastTurnitinError = response.statusMessage.getOrElse("Failed to retrieve results")
				report.nextResponseAttempt = nextResponseAttempt(report).orNull
				logger.info(s"Next attempt will be at ${report.nextResponseAttempt}")
		}

		originalityReportService.saveOrUpdate(report)
	}

	private def saveReport(report: OriginalityReport, result: SubmissionResults): Unit = {
		report.similarity = result.similarity
		report.overlap = result.overlap.map(_.toInt)
		report.publicationOverlap = result.publication_overlap.map(_.toInt)
		report.webOverlap = result.web_overlap.map(_.toInt)
		report.studentOverlap = result.student_overlap.map(_.toInt)
	}

	private def finish(): Result = {
		val completedAssignments = queue.completedAssignments
		val failedAssignments = queue.failedAssignments

		if (completedAssignments.nonEmpty) logger.info(s"Completed assignments: ${completedAssignments.map(_.id).mkString(", ")}")
		if (failedAssignments.nonEmpty) logger.info(s"Failed assignments: ${failedAssignments.map(_.id).mkString(", ")}")
		(completedAssignments ++ failedAssignments).foreach { a =>
			a.submitToTurnitin = false
			assessmentService.save(a)
		}

		Result(completedAssignments, failedAssignments)
	}

	private def nextAttempt(assignment: Assignment): Option[DateTime] =
		Option(assignment)
			.filter(_.submitToTurnitinRetries < MaxAssignmentAttempts)
			.map(r => DateTime.now.plusSeconds(AttemptInterval.toSeconds.toInt * Math.pow(2, r.submitToTurnitinRetries.toInt).toInt))

	private def nextSubmitAttempt(report: OriginalityReport): Option[DateTime] =
		Option(report)
			.filter(_.submitAttempts < MaxSubmitAttempts)
			.map(r => DateTime.now.plusSeconds(AttemptInterval.toSeconds.toInt * Math.pow(2, r.submitAttempts - 1).toInt))

	private def nextResponseAttempt(report: OriginalityReport): Option[DateTime] =
		Option(report)
			.filter(_.responseAttempts < MaxResponseAttempts)
			.map(r => DateTime.now.plusSeconds(AttemptInterval.toSeconds.toInt * Math.pow(2, r.responseAttempts - 1).toInt))
}

trait ProcessTurnitinLtiBackoffQueuePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

}

trait ProcessTurnitinLtiBackoffQueueNotification extends Notifies[Result, Assignment] {
	self: TurnitinLtiBackoffQueueServiceComponent =>

	def queue: TurnitinLtiBackoffQueueService = turnitinLtiBackoffQueueService

	override def emit(result: Result): Seq[Notification[OriginalityReport, Assignment]] = {
		val completedNotifications = result.completed
			.filterNot(_.submissions.isEmpty)
			.flatMap(assignment => {
				val completedReports = queue.originalityReports(assignment).filter(report =>
					report.reportReceived || (!report.reportReceived && (
						report.submitToTurnitinRetries == TurnitinLtiService.SubmitAttachmentMaxRetries ||
							report.reportRequestRetries == TurnitinLtiService.ReportRequestMaxRetries
						))
				)
				assignment.turnitinLtiNotifyUsers.map(user =>
					Notification.init(new TurnitinJobSuccessNotification, user, completedReports, assignment)
				)
			})

		val failedNotifications = result.failed.flatMap(assignment => {
			assignment.turnitinLtiNotifyUsers.map(user =>
				Notification.init(new TurnitinJobErrorNotification, user, Seq[OriginalityReport](), assignment)
			)
		})

		completedNotifications ++ failedNotifications
	}
}
