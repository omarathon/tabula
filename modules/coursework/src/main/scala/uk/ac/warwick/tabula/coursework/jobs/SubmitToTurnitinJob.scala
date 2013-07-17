package uk.ac.warwick.tabula.coursework.jobs

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.coursework.commands.turnitin.TurnitinTrait
import uk.ac.warwick.tabula.data.model.{Assignment, OriginalityReport}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.coursework.services.turnitin.Turnitin._
import uk.ac.warwick.tabula.coursework.services.turnitin._
import uk.ac.warwick.tabula.web.views.{FreemarkerTextRenderer, FreemarkerRendering}
import uk.ac.warwick.tabula.jobs._
import java.util.HashMap
import uk.ac.warwick.tabula.services.OriginalityReportService
import language.implicitConversions
import uk.ac.warwick.tabula.coursework.jobs.notifications.{TurnitinJobSuccessNotification, TurnitinJobErrorNotification}

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
class SubmitToTurnitinJob extends Job
	with TurnitinTrait with NotifyingJob[Seq[TurnitinSubmissionInfo]] with Logging with FreemarkerRendering {

	val identifier = SubmitToTurnitinJob.identifier

	@Autowired var assignmentService: AssignmentService = _
	@Autowired var originalityReportService: OriginalityReportService = _

	val WaitingRetries = 50
	val WaitingSleep = 20000

	var sendNotifications = true

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
		val existingSubmissions = session.listSubmissions(classId, className, assignmentId, assignmentName) match {
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
				if (sendNotifications) {
					debug("Sending an email to " + job.user.email)
					addNotification(new TurnitinJobErrorNotification(assignment, job.user.apparentUser) with FreemarkerTextRenderer)
				}
				throw new FailedJobException("Failed to get list of existing submissions: " + failure)
			}
		}

		def run() {
			updateStatus("Submitting to Turnitin")
			updateProgress(10) // update the progress bar

			removeDefunctSubmissions()
			val uploadedSubmissions = uploadSubmissions
			retrieveReport(uploadedSubmissions._1, uploadedSubmissions._2)
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
					debug(s"Deleting submission that no longer exists in app: title=${info.title} objectId=${info.objectId} ${info.universityId}")
					val deleted = session.deleteSubmission(classId, assignmentId, info.objectId)
					debug(deleted.toString)
				} else {
					debug(s"Submission still exists so not deleting: title/id=${info.title}")
				}
			}
		}

		def uploadSubmissions(): (HashMap[String, String], Int) = {
			var uploadsDone = 0
			var uploadsFailed = 0
			var failedUploads = new HashMap[String, String]
			val allAttachments = assignment.submissions flatMap { _.allAttachments }
			val uploadsTotal = allAttachments.size

			assignment.submissions foreach { submission =>
				debug("Submission ID: " + submission.id)
				debug("submission.allAttachments: (" + submission.allAttachments.size + ")")

				for (attachment <- submission.allAttachments if Turnitin.validFileType(attachment)) {
					val alreadyUploaded = existingSubmissions.exists(_.matches(attachment))
					if (alreadyUploaded) {
						// we don't need to upload it again, probably
						debug("Not uploading attachment again because it looks like it's been uploaded before:" +
								s" ${attachment.name}(by ${submission.universityId})")
					} else {
						debug(s"Uploading attachment: ${attachment.name} (by ${submission.universityId}). Paper title: ${attachment.id}")
						val submitResponse = session.submitPaper(
								classId, 
								className, 
								assignmentId, 
								assignmentName, 
								attachment.id, 
								attachment.name, 
								attachment.file, 
								submission.universityId, 
								"Student")
								
						debug("submitResponse: " + submitResponse)
						if (!submitResponse.successful) {
							//throw new FailedJobException("Failed to upload '" + attachment.name +"' - " + submitResponse.message)
							logger.warn("Failed to upload '" + attachment.name +"' - " + submitResponse.message)
							uploadsFailed += 1
							failedUploads.put(attachment.name, submitResponse.message)
						}
					}
					uploadsDone += 1
				}

				updateProgress(10 + ((uploadsDone * 80) / uploadsTotal)) // 10% - 90%
			}

			debug("Done uploads (" + uploadsDone + ")")
			(failedUploads, uploadsTotal)
		}
		
		

		def retrieveReport(failedUploads: HashMap[String, String], uploadsTotal: Int) {
			// Uploaded all the submissions probably, now we wait til they're all checked
			updateStatus("Waiting for documents to be checked...")

			// try WaitingRetries times with a WaitingSleep msec sleep inbetween until we get a full Turnitin report.
			val reports = runCheck(WaitingRetries) getOrElse Nil

			if (reports.isEmpty && failedUploads.size() != uploadsTotal) {
				logger.error("Waited for complete Turnitin report but didn't get one.")
				updateStatus("Failed to generate a report. The service may be busy - try again later.")
				if (sendNotifications) {
					debug("Sending an email to " + job.user.email)
					addNotification(new TurnitinJobErrorNotification(assignment, job.user.apparentUser) with FreemarkerTextRenderer)
				}
			} else {

				val attachments = assignment.submissions.flatMap(_.allAttachments)
				
				for (report <- reports) {

					val matchingAttachment = attachments.find(attachment => report.matches(attachment))

					matchingAttachment match {
						case Some(attachment) => {
							// FIXME this is a clunky way to replace an existing report
							if (attachment.originalityReport != null) {
								originalityReportService.deleteOriginalityReport(attachment)
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
							originalityReportService.saveOriginalityReport(attachment)
						}
						case None => logger.warn("Got plagiarism report for %s but no corresponding Submission item" format (report.universityId))
					}
		
				}

				if (sendNotifications) {
					debug("Sending an email to " + job.user.email)
					addNotification(new TurnitinJobSuccessNotification(failedUploads, reports, assignment, job.user.apparentUser) with FreemarkerTextRenderer)
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
			// getSubmissions isn't recursive, it just makes the code after it clearer.
			def getSubmissions() = {
				Thread.sleep(WaitingSleep)
				session.listSubmissions(classId, className, assignmentId, assignmentName) match {
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

	def loginFailure = new IllegalStateException("Failed to login user to Turnitin")
}