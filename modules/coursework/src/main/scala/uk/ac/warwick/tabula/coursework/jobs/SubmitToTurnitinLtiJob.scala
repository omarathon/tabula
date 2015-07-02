package uk.ac.warwick.tabula.coursework.jobs


import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.coursework.commands.turnitin.TurnitinTrait
import uk.ac.warwick.tabula.data.model.{FileAttachment, FileAttachmentToken, Assignment}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.coursework.services.turnitin.Turnitin._
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.tabula.jobs._
import language.implicitConversions
import uk.ac.warwick.tabula.services.turnitinlti.{AutowiringTurnitinLtiServiceComponent, TurnitinLtiService}
import scala.collection.JavaConverters._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.coursework.services.turnitin.TurnitinSubmissionInfo
import uk.ac.warwick.tabula.jobs.JobPrototype
import uk.ac.warwick.tabula.coursework.web.Routes

object SubmitToTurnitinLtiJob {
	val identifier = "turnitin-submit-lti"
	def apply(assignment: Assignment) = JobPrototype(identifier, Map(
		"assignment" -> assignment.id))
}

@Component
class SubmitToTurnitinLtiJob extends Job
	with TurnitinTrait with NotifyingJob[Seq[TurnitinSubmissionInfo]] with Logging with FreemarkerRendering
	with AutowiringAssessmentServiceComponent with AutowiringTurnitinLtiServiceComponent
	with AutowiringFileAttachmentServiceComponent with AutowiringOriginalityReportServiceComponent {

	val identifier = SubmitToTurnitinLtiJob.identifier

	var topLevelUrl: String = Wire.property("${toplevel.url}")

	val WaitingRetries = 5
	val WaitingSleep = 20000

	var sendNotifications = true

	def run(implicit job: JobInstance) {
		new Runner(job).run()
	}

	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		val assignment = {
			transactional() {
			val id = job.getString("assignment")
			assessmentService.getAssignmentById(id) getOrElse (throw obsoleteJob)
		}}
		val department = assignment.module.adminDepartment
		val classId = classIdFor(assignment, api.classPrefix)
		val assignmentId = assignmentIdFor(assignment)
		val className = classNameFor(assignment)
		val assignmentName = assignmentNameFor(assignment)

		def run() {
			updateStatus("Submitting to Turnitin...")
			updateProgress(10) // update the progress bar

			val classId = TurnitinLtiService.classIdFor(assignment, turnitinLtiService.classPrefix)
			val assignmentId = TurnitinLtiService.assignmentIdFor(assignment)
			val className = TurnitinLtiService.classNameFor(assignment)
			val assignmentName = TurnitinLtiService.assignmentNameFor(assignment)

			debug(s"Submitting assignment in ${classId.value}, ${assignmentId.value}")

//			val userEmail = if (user.email == null || user.email.isEmpty) user.firstName + user.lastName + "@TurnitinLti.warwick.ac.uk" else user.email

			val submitAssignmentResponse = turnitinLtiService.submitAssignment(assignmentId, assignmentName, classId, className, job.user)

			if (!submitAssignmentResponse.success){
				throw new FailedJobException("Failed to submit assignment '" + assignment.name +"' - " + Some(submitAssignmentResponse.statusMessage))
			}

			var retries = 0
			var allPapersSubmitted = false
			var includesFailedSubmissions = false

			while (!allPapersSubmitted & retries <= WaitingRetries) {
				Thread.sleep(WaitingSleep)
				if (assignment.turnitinId.nonEmpty) {
					assignment.submissions.asScala.foreach(submission => {
						submission.allAttachments.foreach(attachment => {
							 val token: FileAttachmentToken = getToken(attachment)

	//						val attachmentAccessUrl = s"$topLevelUrl/scheduling/turnitin/submission/${submission.id}/attachment/${attachment.id}/test.doc?token=${token.id}"
//							val attachmentAccessUrl = "https://files.warwick.ac.uk/sarahotoole/files/ab/test.doc"

							val attachmentAccessUrl = Routes.admin.assignment.turnitinlti.fileByToken(submission, attachment, token)

							// TODO we need to ensure we don't resubmit the same papers again.

							// not actually the email firstname and lastname of the student, as per existing job...
							// check that the response was successful
							val submitResponse = turnitinLtiService.submitPaper(assignment.turnitinId, attachmentAccessUrl, s"${submission.userId}@TurnitinLti.warwick.ac.uk",
																															submission.userId, "Student")
							// TODO keep track of failed uploads
							debug("submitResponse: " + submitResponse)
							if (!submitResponse.success) {
								//throw new FailedJobException("Failed to upload '" + attachment.name +"' - " + submitResponse.message)
								logger.warn("Failed to upload '" + attachment.name + "' - " + submitResponse.statusMessage)
								includesFailedSubmissions = true
							} else {
								logger.info("turnitin submission id: " + submitResponse.turnitinSubmissionId)
							}
						})
					})

					allPapersSubmitted = true
					// TODO need to go through each one, get results from Turnigin, then update the OriginalityReport
					// TODO - set job as successful once we have a report for each submission
					retries = WaitingRetries
				}
				retries += 1
			}

			transactional() {
				job.succeeded = allPapersSubmitted && !includesFailedSubmissions
			}

		}

		def getToken(attachment: FileAttachment): FileAttachmentToken = {
			transactional() {
				val token = attachment.generateToken()
				fileAttachmentService.saveOrUpdate(token)
				token
			}
		}
	}
}