package uk.ac.warwick.courses.jobs

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.MimeMailMessage
import org.springframework.stereotype.Component
import freemarker.template.Configuration
import javax.annotation.Resource
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.Describable
import uk.ac.warwick.courses.commands.turnitin.TurnitinTrait
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.OriginalityReport
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.services.jobs.JobInstance
import uk.ac.warwick.courses.services.turnitin.Turnitin._
import uk.ac.warwick.courses.services.turnitin._
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
		new Runner(job).run()
	}

	// Job is a shared service, so rather than pass objects between methods,
	// let's use an inner class.
	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		val assignment = {
			val id = job.getString("assignment")
			assignmentService.getAssignmentById(id) getOrElse (throw obsoleteJob)
		}
		val classId = classIdFor(assignment)
		val assignmentId = assignmentIdFor(assignment)
		val className = classNameFor(assignment)
		val assignmentName = assignmentNameFor(assignment)

		lazy val session = api.login(job.user).getOrElse(throw loginFailure)

		// Get existing submissions. 
		val existingSubmissions = session.listSubmissions(classId, assignmentId) match {
			case ClassNotFound() | AssignmentNotFound() => { // class or assignment don't exist, so clearly there are no submissions yet.
				// ensure assignment and course are created, as submitPaper doesn't always do that for you
				session.createAssignment(classId, className, assignmentId, assignmentName)
				Nil 
			}
			case GotSubmissions(list) => list
			case _ => Nil // FIXME this is probably an error response, don't just assume it's an empty collection.
		}

		def run() {
			updateStatus("Submitting to Turnitin")
			updateProgress(10) // update the progress bar

			removeDefunctSubmissions()
			uploadSubmissions()
			retrieveReport()
		}

		def removeDefunctSubmissions() {
			// delete files in turnitin that aren't in the assignment any more
			for (info <- existingSubmissions) {
				val exists = assignment.submissions exists { submission =>
					submission.allAttachments.exists { attachment =>
						attachment.id == info.title // title is used to store ID
					}
				}
				if (!exists) {
					debug("Deleting submission that no longer exists in app: title="+info.title+" objectId=" + info.objectId + " " + info.universityId)
					val deleted = session.deleteSubmission(classId, assignmentId, info.objectId)
					debug(deleted.toString)
				}
			}
		}

		def uploadSubmissions() {
			var uploadsDone = 0
			val allAttachments = assignment.submissions flatMap { _.allAttachments }
			val uploadsTotal = allAttachments.size

			assignment.submissions foreach { submission =>
				debug("Submission ID: " + submission.id)
				debug("submission.allAttachments: (" + submission.allAttachments.size + ")")

				submission.allAttachments foreach { attachment =>
					val alreadyUploaded = existingSubmissions.exists(info => info.universityId == submission.universityId && info.title == attachment.name)
					if (alreadyUploaded) {
						// we don't need to upload it again, probably
						debug("Not uploading attachment again because it looks like it's been uploaded before: " + attachment.name + "(by " + submission.universityId + ")")
					} else {
						debug("Uploading attachment: " + attachment.name + " (by " + submission.universityId + ")")
						val submitResponse = session.submitPaper(classId, className, assignmentId, assignmentName, attachment.id, attachment.name, attachment.file, submission.universityId, "Student")
						debug("submitResponse: " + submitResponse)
						if (!submitResponse.successful) {
							debug("Failed to upload document " + attachment.name)
							throw new FailedJobException("Failed to upload '" + attachment.name +"' - " + submitResponse.message)
						}
					}
					uploadsDone += 1
				}

				updateProgress(10 + ((uploadsDone * 80) / uploadsTotal)) // 10% - 90%
			}

			debug("Done uploads (" + uploadsDone + ")")
		}
		
		

		def retrieveReport() {
			// Uploaded all the submissions probably, now we wait til they're all checked
			updateStatus("Waiting for documents to be checked...")

			// try WaitingRetries times with a WaitingSleep msec sleep inbetween until we get a full Turnitin report.
			val reports = runCheck(WaitingRetries) getOrElse Nil

			if (reports.isEmpty) {
				logger.error("Waited for complete Turnitin report but didn't get one.")
				updateStatus("Failed to generate a report. The service may be busy - try again later.")
			} else {

				val attachments = assignment.submissions.flatMap(_.allAttachments)
				
				for (report <- reports) {

					val matchingAttachment = attachments.find(attachment => report.matches(attachment))

					matchingAttachment match {
						case Some(attachment) => {
							// FIXME this is a clunky way to replace an existing report
							if (attachment.originalityReport != null) {
								assignmentService.deleteOriginalityReport(attachment)
							}
							val r = {
								val r = new OriginalityReport
								r.similarity = Some(report.similarityScore)
								r.overlap = report.overlap
								r.webOverlap = report.webOverlap
								r.publicationOverlap = report.publicationOverlap
								r.studentOverlap = report.studentPaperOverlap
								r
							}
							attachment.originalityReport = r
							assignmentService.saveOriginalityReport(attachment)
						}
						case None => logger.warn("Got plagiarism report for %s but no corresponding Submission item" format (report.universityId))
					}
		
				}

				if (sendEmails) {
					debug("Sending an email to " + job.user.email)
					val mime = mailer.createMimeMessage()
					val email = new MimeMailMessage(mime)
					email.setFrom(replyAddress)
					email.setTo(job.user.email)
					email.setSubject("Turnitin check finished for %s - %s" format (assignment.module.code.toUpperCase, assignment.name))
					email.setText(renderEmailText(job.user, assignment))
					mailer.send(mime)
				}

				updateStatus("Generated a report.")
				updateProgress(100)
				job.succeeded = true
			}
		}

		/**
		 * Recursive function to repeatedly check if all the documents have been scored yet.
		 * If we run out of attempts we return None. Otherwise we return Some(list), so you can
		 * distinguish between a timeout and an actual empty collection.
		 * look, it's one of those recursive functions we've learned about
		 */
		@tailrec
		private def runCheck(retries: Int): Option[Seq[TurnitinSubmissionInfo]] = {
			def attemptCheck() = {
				Thread.sleep(WaitingSleep)
				session.listSubmissions(classId, assignmentId) match {
					case GotSubmissions(list) => {
						val checked = list filter { _.hasBeenChecked }
						if (checked.size == list.size) {
							// all checked
							updateStatus("All documents checked, preparing report...")
							Some(list)
						} else {
							logger.debug("Waiting for documents to be checked (%d/%d)..." format (checked.size, list.size))
							updateStatus("Waiting for documents to be checked (%d/%d)..." format (checked.size, list.size))
							None
						}
					}
					case _ => None
				}
			}

			if (retries == 0) None
			else {
				attemptCheck() match {
					case Some(list) => Some(list)
					case None => runCheck(retries - 1)
				}
			}

		}

	}

	def renderEmailText(user: CurrentUser, assignment: Assignment) = {
		renderToString("/WEB-INF/freemarker/emails/turnitinjobdone.ftl", Map(
			"assignment" -> assignment,
			"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
			"user" -> user,
			"url" -> (topLevelUrl + Routes.admin.assignment.submission(assignment))))
	}

	def loginFailure = new IllegalStateException("Failed to login user to Turnitin")

}