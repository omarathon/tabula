package uk.ac.warwick.courses.jobs

import scala.collection.JavaConversions._

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.MimeMailMessage
import org.springframework.stereotype.Component

import freemarker.template.Configuration
import javax.annotation.Resource
import javax.persistence.Entity
import javax.persistence.Table
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.Describable
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.commands.turnitin.TurnitinTrait
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.OriginalityReport
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.services.jobs.JobInstance
import uk.ac.warwick.courses.services.turnitin.GotSubmissions
import uk.ac.warwick.courses.services.turnitin.TurnitinSubmissionInfo
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.web.views.FreemarkerRendering
import uk.ac.warwick.util.mail.WarwickMailSender

object SubmitToTurnitinJob {
	val identifier = "turnitin-submit"
	def apply(assignment: Assignment) = JobPrototype(identifier, Map(
		"assignment" -> assignment.id))
}

abstract class DescribableJob(instance: JobInstance) extends Describable[Nothing] {
	val eventName: String = instance.getClass.getSimpleName.replaceAll("Job$", "")
}

/**
 * This job uses the Turnitin service to upload an assignment's submission attachments,
 * then wait for a while until Turnitin has done plagiarism detection on them all and
 * then returns a report by email, and attached the report to the submissions.
 *
 * The submission part should be fairly safe to repeat, i.e. you can run it more than
 * once and it won't cause duplicates and problems. This should be true even if the job
 * only part-finished last time (like we uploaded half of the submissions). Obviously
 * it will send an email every time.
 *
 * FIXME send email!
 */
@Component
class SubmitToTurnitinJob extends Job with TurnitinTrait with Logging with FreemarkerRendering {

	val identifier = SubmitToTurnitinJob.identifier

	@Autowired implicit var freemarker: Configuration = _
	@Autowired var assignmentService: AssignmentService = _
	@Resource(name = "mailSender") var mailer: WarwickMailSender = _

	@Value("${mail.noreply.to}") var replyAddress: String = _
	@Value("${mail.exceptions.to}") var fromAddress: String = _
	@Value("${toplevel.url}") var topLevelUrl: String = _

	val WaitingRetries = 50
	val WaitingSleep = 20000

	var sendEmails = true

	def run(implicit job: JobInstance) {
		status = "Submitting to Turnitin"
		val id = job.getString("assignment")
		val assignment = assignmentService.getAssignmentById(id) getOrElse (throw obsoleteJob)
		val assignmentId = createOrGetAssignment(assignment)
		if (debugEnabled) logger.debug("Assignment ID: " + assignmentId.getOrElse("UNKNOWN!!"))
		assignmentId map { id =>

			// get a list of already-submitted items here and either ignore them or delete them
			val existingSubmissions = api.listSubmissions(classNameFor(assignment), assignmentNameFor(assignment)) match {
				case GotSubmissions(list) => list
				case _ => Nil // FIXME this is probably an error response, don't just assume it's an empty collection.
			}

			progress = 10

			removeDefunctSubmissions(assignment, existingSubmissions)
			uploadSubmissions(assignment, existingSubmissions)
			retrieveReport(id, assignment)

		}
	}

	def removeDefunctSubmissions(assignment: Assignment, existingSubmissions: Seq[TurnitinSubmissionInfo]) {
		// delete files in turnitin that aren't in the assignment any more
		for (info <- existingSubmissions) {
			val exists = assignment.submissions exists { submission =>
				submission.allAttachments.exists { attachment =>
					info.universityId == submission.universityId && info.title == attachment.name
				}
			}
			if (!exists) {
				logger.debug("Deleting submission that no longer exists in app: objectId=" + info.objectId + " " + info.universityId)
				val deleted = api.deleteSubmission(classNameFor(assignment), assignmentNameFor(assignment), info.objectId)
				logger.debug(deleted)
			}
		}
	}

	def uploadSubmissions(assignment: Assignment, existingSubmissions: Seq[TurnitinSubmissionInfo])(implicit job: JobInstance) {
		var uploadsDone = 0
		val allAttachments = assignment.submissions flatMap { _.allAttachments }
		val uploadsTotal = allAttachments.size

		assignment.submissions foreach { submission =>
			if (debugEnabled) logger.debug("Submission ID: " + submission.id)
			logger.debug("submission.allAttachments: (" + submission.allAttachments.size + ")")

			submission.allAttachments foreach { attachment =>
				val alreadyUploaded = existingSubmissions.exists(info => info.universityId == submission.universityId && info.title == attachment.name)
				if (alreadyUploaded) {
					// we don't need to upload it again, probably
					if (debugEnabled) logger.debug("Not uploading attachment again because it looks like it's been uploaded before: " + attachment.name + "(by " + submission.universityId + ")")
				} else {
					if (debugEnabled) logger.debug("Uploading attachment: " + attachment.name + " (by " + submission.universityId + ")")
					val submitResponse = api.submitPaper(classNameFor(assignment), assignmentNameFor(assignment), attachment.name, attachment.file, submission.universityId, "Student")
					if (debugEnabled) logger.debug("submitResponse: " + submitResponse)
					if (!submitResponse.successful) {
						logger.debug("Failed to upload document " + attachment.name)
					}
				}
				uploadsDone += 1
			}

			progress = (10 + ((uploadsDone * 80) / uploadsTotal)) // 10% - 90%
		}

		logger.debug("Done uploads (" + uploadsDone + ")")
	}

	def retrieveReport(turnitinId: String, assignment: Assignment)(implicit job: JobInstance) {
		// Uploaded all the submissions probably, now we wait til they're all checked
		status = "Waiting for documents to be checked..."

		// try WaitingRetries times with a WaitingSleep msec sleep inbetween until we get a full Turnitin report.
		val submissions = runCheck(assignment, WaitingRetries) getOrElse Nil

		if (submissions.isEmpty) {
			logger.error("Waited for complete Turnitin report but didn't get one. Turnitin assignment: " + turnitinId)
			status = "Failed to generate a report. The service may be busy - try again later."
		} else {

			for (report <- submissions) {
				val submission = assignment.submissions.find(s => s.universityId == report.universityId)
				submission.map { submission =>
					// FIXME this is a clunky way to replace an existing report
					if (submission.originalityReport != null) {
						assignmentService.deleteOriginalityReport(submission)
					}
					submission.addOriginalityReport({
						val r = new OriginalityReport
						r.similarity = Some(report.similarityScore)
						r.overlap = report.overlap
						r.webOverlap = report.webOverlap
						r.publicationOverlap = report.publicationOverlap
						r.studentOverlap = report.studentPaperOverlap
						r
					})
					assignmentService.saveSubmission(submission)
				} getOrElse {
					logger.warn("Got plagiarism report for %s but no corresponding Submission item" format (report.universityId))
				}
			}

			if (sendEmails) {
				logger.debug("Sending an email to " + job.user.email)
				val mime = mailer.createMimeMessage()
				val email = new MimeMailMessage(mime)
				email.setFrom(replyAddress)
				email.setTo(job.user.email)
				email.setSubject("Turnitin check finished for %s - %s" format (assignment.module.code.toUpperCase, assignment.name))
				email.setText(renderEmailText(job.user, assignment))
				mailer.send(mime)
			}

			status = "Generated a report."
			progress = 100
			job.succeeded = true
		}
	}

	def renderEmailText(user: CurrentUser, assignment: Assignment) = {
		renderToString("/WEB-INF/freemarker/emails/turnitinjobdone.ftl", Map(
			"assignment" -> assignment,
			"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
			"user" -> user,
			"url" -> (topLevelUrl + Routes.admin.assignment.submission(assignment))))
	}

	/**
	 * Recursive function to repeatedly check if all the documents have been scored yet.
	 * If we run out of attempts we return None. Otherwise we return Some(list), so you can
	 * distinguish between a timeout and an actual empty collection.
	 */
	def runCheck(assignment: Assignment, retries: Int)(implicit job: JobInstance): Option[Seq[TurnitinSubmissionInfo]] = {
		if (retries == 0) None
		else runCheck(assignment) orElse runCheck(assignment, retries - 1)
	}

	def runCheck(assignment: Assignment)(implicit job: JobInstance): Option[Seq[TurnitinSubmissionInfo]] = {
		Thread.sleep(WaitingSleep)
		api.listSubmissions(classNameFor(assignment), assignment.id) match {
			case GotSubmissions(list) => {
				val checked = list filter { _.hasBeenChecked }
				if (checked.size == list.size) {
					// all checked
					status = "All documents checked, preparing report..."
					Some(list)
				} else {
					status = ("Waiting for documents to be checked (%d/%d)..." format (checked.size, list.size))
					None
				}
			}
			case _ => None
		}
	}

}