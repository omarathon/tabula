package uk.ac.warwick.tabula.coursework.jobs

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.MimeMailMessage
import org.springframework.stereotype.Component
import freemarker.template.Configuration
import javax.annotation.Resource
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.coursework.commands.turnitin.TurnitinTrait
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.OriginalityReport
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.coursework.services.turnitin.Turnitin._
import uk.ac.warwick.tabula.coursework.services.turnitin._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.tabula.jobs._

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
		val classId = classIdFor(assignment, api.classPrefix)
		val assignmentId = assignmentIdFor(assignment)
		val className = classNameFor(assignment)
		val assignmentName = assignmentNameFor(assignment)

		lazy val session = api.login(job.user).getOrElse(throw loginFailure)

		// Get existing submissions. 
		val existingSubmissions = session.listSubmissions(classId, assignmentId) match {
			case ClassNotFound() | AssignmentNotFound() => { // class or assignment don't exist
				// ensure assignment and course are created, as submitPaper doesn't always do that for you
				debug("Missing class or assignment, creating...")
				session.createAssignment(classId, className, assignmentId, assignmentName)
				Nil // clearly there are no submissions yet.
			}
			case GotSubmissions(list) => {
				debug("Got list of " + list.size + " existing submissions: " + list.map(_.title))
				list
			}
			case failure => {
				if (sendEmails) {
					debug("Sending an email to " + job.user.email)
					val mime = mailer.createMimeMessage()
					val email = new MimeMailMessage(mime)
					email.setFrom(replyAddress)
					email.setTo(job.user.email)
					email.setSubject("Turnitin check has not completed successfully for %s - %s" format (assignment.module.code.toUpperCase, assignment.name))
					email.setText(renderJobFailedEmailText(job.user, assignment))
					mailer.send(mime)
				}
				throw new FailedJobException("Failed to get list of existing submissions: " + failure)
			}
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
				val exists = assignment.submissions.exists { submission =>
					submission.allAttachments.exists { attachment =>
						attachment.id == info.title // title is used to store ID
					}
				}
				if (!exists) {
					debug("Deleting submission that no longer exists in app: title="+info.title+" objectId=" + info.objectId + " " + info.universityId)
					val deleted = session.deleteSubmission(classId, assignmentId, info.objectId)
					debug(deleted.toString)
				} else {
					debug("Submission still exists so not deleting: title/id="+info.title)
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

				for (attachment <- submission.allAttachments if Turnitin.validFileType(attachment)) {
					val alreadyUploaded = existingSubmissions.exists(_.matches(attachment))
					if (alreadyUploaded) {
						// we don't need to upload it again, probably
						debug("Not uploading attachment again because it looks like it's been uploaded before: " + attachment.name + "(by " + submission.universityId + ")")
					} else {
						debug("Uploading attachment: " + attachment.name + " (by " + submission.universityId + "). Paper title: " + attachment.id)
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
				if (sendEmails) {
					debug("Sending an email to " + job.user.email)
					val mime = mailer.createMimeMessage()
					val email = new MimeMailMessage(mime)
					email.setFrom(replyAddress)
					email.setTo(job.user.email)
					email.setSubject("Turnitin check has not completed successfully for %s - %s" format (assignment.module.code.toUpperCase, assignment.name))
					email.setText(renderJobFailedEmailText(job.user, assignment))
					mailer.send(mime)
				}
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
					email.setText(renderJobDoneEmailText(job.user, assignment))
					mailer.send(mime)
				}

				updateStatus("Generated a report.")
				job.succeeded = true
				updateProgress(100)
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
			// getSubmissions isn't recusive, it just makes the code after it clearer.
			def getSubmissions() = {
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
					case a => {
						debug("listSubmissions returned " + a)
						None
					}
				}
			}

			if (retries == 0) None
			else getSubmissions() match {
				case Some(list) => Some(list)
				case None => runCheck(retries - 1)
			}
		}

	}

	def renderJobDoneEmailText(user: CurrentUser, assignment: Assignment) = {
		renderToString("/WEB-INF/freemarker/emails/turnitinjobdone.ftl", Map(
			"assignment" -> assignment,
			"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
			"user" -> user,
			"path" -> Routes.admin.assignment.submissionsandfeedback(assignment)
		))
	}
	
	def renderJobFailedEmailText(user: CurrentUser, assignment: Assignment) = {
		renderToString("/WEB-INF/freemarker/emails/turnitinjobfailed.ftl", Map(
			"assignment" -> assignment,
			"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
			"user" -> user,
			"path" -> Routes.admin.assignment.submissionsandfeedback(assignment)
		))
	}

	def loginFailure = new IllegalStateException("Failed to login user to Turnitin")

}