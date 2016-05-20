package uk.ac.warwick.tabula.jobs.coursework


import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.tabula.jobs._
import uk.ac.warwick.tabula.services.turnitinlti.{TurnitinLtiResponse, TurnitinLtiService, AutowiringTurnitinLtiServiceComponent}
import scala.collection.JavaConverters._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.jobs.JobPrototype
import uk.ac.warwick.tabula.data.model.notifications.coursework.{TurnitinJobSuccessNotification, TurnitinJobErrorNotification}
import scala.annotation.tailrec
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, CurrentUser}

object SubmitToTurnitinLtiJob {
	val identifier = "turnitin-submit-lti"
	def apply(assignment: Assignment) = JobPrototype(identifier, Map(
		"assignment" -> assignment.id))
}

@Component
class SubmitToTurnitinLtiJob extends Job
	with NotifyingJob[Seq[OriginalityReport]]
  with Logging with FreemarkerRendering
	with AutowiringAssessmentServiceComponent with AutowiringTurnitinLtiServiceComponent
	with AutowiringFileAttachmentServiceComponent with AutowiringOriginalityReportServiceComponent
	with AutowiringFeaturesComponent {

	val identifier = SubmitToTurnitinLtiJob.identifier

	var topLevelUrl: String = Wire.property("${toplevel.url}")

	// Turnitin have requested that submissions should be sent at a rate of no more than 1 per second
	val WaitingRequestsToTurnitinSleep = 20000
	val WaitingRequestsToTurnitinRetries = 50

	val WaitingRequestsToTurnitinSubmitPaperSleep = 2000
	val WaitingRequestsToTurnitinSubmitPaperRetries = 20

	val WaitingRequestsFromTurnitinCallbackSleep = 2000
	val WaitingRequestsFromTurnitinCallbacksRetries = 20

	var sendNotifications = true

	def run(implicit job: JobInstance) {
		if (features.turnitinSubmissions) {
			new Runner(job).run()
		}
	}

	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		val assignment = {
			transactional(readOnly = true) {
			val id = job.getString("assignment")
			assessmentService.getAssignmentById(id) getOrElse (throw obsoleteJob)
		}}

		def run() {
			updateStatus("Submitting to Turnitin...")
			updateProgress(10) // update the progress bar

			submitAssignment(WaitingRequestsToTurnitinRetries)

			// Re-get this assignmnet from hibernate, to ensure we can retrieve the Turnitin id (set in a separate transaction).
			// Otherwise we can get "Assignment LineItem not found" from Turnitin when submitting a paper
			logger.info(s"$logPrefixWithJobId Awaiting Turnitin ID")
			val assignmentWithTurnitinId = awaitTurnitinId(assignment, WaitingRequestsFromTurnitinCallbacksRetries)

			updateStatus("Submitting papers to Turnitin")
			logger.info(s"$logPrefixWithJobId Submitting papers to Turnitin")
			val allAttachments = assignment.submissions.asScala flatMap { _.allAttachments.filter(TurnitinLtiService.validFile) }
			val uploadsTotal = allAttachments.size

			val failedUploads =
				if (uploadsTotal > 0) submitPapers(assignmentWithTurnitinId, uploadsTotal)
				else Map.empty[FileAttachment, String]

			updateStatus("Getting similarity reports from Turnitin")
			logger.info(s"s$logPrefixWithJobId Getting similarity reports from Turnitin")
			val (originalityReports, noResults) =
				if (uploadsTotal > 0) retrieveResults(uploadsTotal, failedUploads.keys.toSeq)
				else (Nil, Map.empty[String, String])

			if (sendNotifications) {
				debug("Sending an email to " + job.user.email)
				val notification = Notification.init(new TurnitinJobSuccessNotification, job.user.apparentUser, originalityReports, assignment)
				// there shouldn't be dupes, but if there are, the first, failed upload, error message is more useful
				notification.failedUploads.value = noResults ++ failedUploads.map { case(fileAttachment, reason) =>
						fileAttachment.name -> reason
				}
				pushNotification(job, notification)
			}
			transactional() {
				updateStatus("Generated a report.")
				job.succeeded = true
				updateProgress(100)
			}
			logger.info(s"$logPrefixWithJobId Submit to Turnitin job completed")
		}

		@tailrec
		private def submitAssignment(retries: Int): TurnitinLtiResponse = {
			def submit()  = {
				logger.info(s"$logPrefixWithJobId Waiting $WaitingRequestsToTurnitinSleep ms before submitting assignment to Turnitin.")
				Thread.sleep(WaitingRequestsToTurnitinSleep)
				logger.info(s"$logPrefixWithJobId Submitting assignment to Turnitin: ${retries} retries remaining")
				turnitinLtiService.submitAssignment(assignment, job.user)
			}
			submit() match {
				case response if response.success => response
				case response if retries == 0 => {
					if (sendNotifications) {
						sendFailureNotification(job, assignment)
					}
					throw new FailedJobException("Failed to submit assignment '" + assignment.name + "' - " + response.statusMessage.getOrElse("Error unknown"))
				}
				case _ => submitAssignment(retries -1)
			}
		}

		private def submitPapers(assignment: Assignment, uploadsTotal: Int): Map[FileAttachment, String] = {

			var failedUploads = Map[FileAttachment, String]()
			var uploadsDone: Int = 0

			assignment.submissions.asScala.foreach(submission => {
				for (attachment <- submission.allAttachments if (TurnitinLtiService.validFile(attachment))) {
					// Don't need to resubmit the same papers again.
					if (attachment.originalityReport == null || !attachment.originalityReport.reportReceived) {
						val attachmentAccessUrl = getAttachmentAccessUrl(submission, attachment)
						val submitPaper = transactional(){
							submitSinglePaper(assignment, attachmentAccessUrl, submission, attachment, WaitingRequestsToTurnitinSubmitPaperRetries)
						}
							if (!submitPaper.success) {
								failedUploads += (attachment -> submitPaper.statusMessage.getOrElse("failed upload"))
							}
					}
					uploadsDone += 1
				}
				updateProgress(10 + ((uploadsDone * 40) / uploadsTotal)) // 10% to 50%
			})

			if (failedUploads.nonEmpty) logger.error(s"$logPrefixWithJobId Not all papers were submitted to Turnitin successfully.")
			failedUploads
		}

		@tailrec
		private def submitSinglePaper(
			assignment: Assignment, attachmentAccessUrl: String, submission: Submission, attachment: FileAttachment, retries: Int
		): TurnitinLtiResponse = {

			def submit() = {
				logger.info(s"$logPrefixWithJobId Waiting $WaitingRequestsToTurnitinSubmitPaperSleep ms before submitting single paper to Turnitin.")
				Thread.sleep(WaitingRequestsToTurnitinSubmitPaperSleep)
				logger.info(s"$logPrefixWithJobId Submitting single paper to Turnitin: $retries retries remaining.")
				turnitinLtiService.submitPaper(assignment, attachmentAccessUrl, submission.userId,
					s"${submission.userId}@TurnitinLti.warwick.ac.uk", attachment, submission.universityId, "Student")
			}

			submit() match {
				case response if response.success =>
						val originalityReport = originalityReportService.getOriginalityReportByFileId(attachment.id)
						if (originalityReport.isDefined) {
							transactional() {
								originalityReport.get.turnitinId = response.turnitinSubmissionId
								originalityReport.get.reportReceived = false
							}
						} else {
							transactional() {
								val report = new OriginalityReport
								report.turnitinId = response.turnitinSubmissionId
								attachment.originalityReport = report
								originalityReportService.saveOriginalityReport(attachment)
							}
						}
					response
				case response if retries == 0 =>
					logger.warn(s"$logPrefixWithJobId Failed to upload ' ${attachment.name} ' - ${response.statusMessage.getOrElse("")}")
					response
				case response if response.statusMessage.isDefined =>
					logger.warn(s"$logPrefixWithJobId Failing to upload '${attachment.name} ' - ${response.statusMessage.getOrElse("")}")
					submitSinglePaper(assignment, getAttachmentAccessUrl(submission, attachment), submission, attachment, 0)
				case _ => {
					submitSinglePaper(assignment, getAttachmentAccessUrl(submission, attachment), submission, attachment, retries - 1)
				}
			}
		}

		def retrieveResults(uploadsTotal: Int, notSubmitted: Seq[FileAttachment]): (Seq[OriginalityReport], Map[String, String]) = {
			var resultsReceived = 0
			val originalityReports = Seq()
			var failedResults = Map[String, String]()
			assignment.submissions.asScala.foreach(submission => {
				submission.allAttachments.filter(TurnitinLtiService.validFile) diff notSubmitted foreach(attachment => {
					val originalityReport = transactional(readOnly = true) {
						originalityReportService.getOriginalityReportByFileId(attachment.id)
					}

					if (originalityReport.isDefined && !originalityReport.get.reportReceived) {
						val report = originalityReport.get

						val response = transactional(readOnly = true) {
							retrieveSinglePaperResults(report.turnitinId, job.user, WaitingRequestsToTurnitinRetries)
						}

						if (response.success && response.submissionInfo.similarity.isDefined) {
							val result = response.submissionInfo
							transactional() {
								report.similarity = result.similarity
								report.overlap = result.overlap.map(_.toInt)
								report.publicationOverlap = result.publication_overlap.map(_.toInt)
								report.webOverlap = result.web_overlap.map(_.toInt)
								report.studentOverlap = result.student_overlap.map(_.toInt)
								attachment.originalityReport = report
								attachment.originalityReport.reportReceived = true
								originalityReportService.saveOriginalityReport(attachment)
								logger.info(s"$logPrefixWithJobId Saving Originality Report with similarity ${result.similarity.getOrElse(None)}")
							}
							originalityReports :+ report
						} else {
							logger.warn(s"$logPrefixWithJobId Failed to get results for ${attachment.id}: ${response.statusMessage.getOrElse("")}")
							failedResults += (attachment.name -> response.statusMessage.getOrElse("failed to retrieve results"))
						}
					} else if(!originalityReport.isDefined) {
						logger.warn(s"$logPrefixWithJobId Failed to find originality report for attachment ${attachment.id}")
						failedResults += (attachment.name -> "failed to find Originality Report")
					}
					resultsReceived += 1
				})

				updateProgress(10 + (((resultsReceived * 40) / uploadsTotal) * 2)) // 50% to 90%
			})
			if (failedResults.nonEmpty) logger.error(s"$logPrefixWithJobId Did not receive all reports from Turnitin.")
			(originalityReports, failedResults)
		}

		@tailrec
		private def retrieveSinglePaperResults(
			turnitinPaperId: String, currentUser: CurrentUser, retries: Int
		): TurnitinLtiResponse = {

			def getResults = {
				logger.info(s"$logPrefixWithJobId Waiting $WaitingRequestsToTurnitinSleep ms before retrieving single paper result from Turnitin.")
				Thread.sleep(WaitingRequestsToTurnitinSleep)
				logger.info(s"$logPrefixWithJobId Retrieving single paper result: $retries retries remaining")
				turnitinLtiService.getSubmissionDetails(turnitinPaperId, currentUser)
			}

			getResults match {
				case response if response.success && response.submissionInfo.similarity.isDefined => response
				case response if retries == 0 => response
				case _ => retrieveSinglePaperResults(turnitinPaperId, currentUser, retries-1)
			}
		}

		private def getAttachmentAccessUrl(submission: Submission, attachment: FileAttachment): String = {
			transactional(readOnly = false, propagation = REQUIRES_NEW) {
				val token = attachment.generateToken()
				fileAttachmentService.saveOrUpdate(token)
				s"$topLevelUrl${Routes.admin.assignment.turnitinlti.fileByToken(submission, attachment, token)}"
			}
		}

		private def sendFailureNotification(job: JobInstance, assignment: Assignment) {
			debug("Sending an email to " + job.user.email)
			val notification = Notification.init(new TurnitinJobErrorNotification, job.user.apparentUser, Seq[OriginalityReport](), assignment)
			pushNotification(job, notification)
		}

		@tailrec
		private def awaitTurnitinId(assignment: Assignment, retries: Int): Assignment = {
			// wait for Callback from Turnitin with Turnitin assignment id - if it already has a turnitin assignment id, that's fine
			def hasTurnitinId = {
				logger.info(s"$logPrefixWithJobId trying to get Turnitin assignment ID: ${retries} retries remaining")
				transactional(readOnly = true, propagation = REQUIRES_NEW) {
					// Re-get this assignment from hibernate as the Turnitin ID has only just been set (in a separate transaction).
					val freshAssignment = assessmentService.getAssignmentById(assignment.id).getOrElse(throw obsoleteJob)
					freshAssignment.turnitinId.hasText
				}
			}

				hasTurnitinId match {
					case true => {
						transactional(){
							assessmentService.getAssignmentById(assignment.id).getOrElse(throw obsoleteJob)
						}
					}
					case false if retries == 0 =>
						if (sendNotifications) {
							sendFailureNotification(job, assignment)
						}
						throw new FailedJobException("Failed to submit the assignment to Turnitin")
					case _ => {
							logger.info(s"$logPrefixWithJobId Waiting $WaitingRequestsFromTurnitinCallbackSleep ms before trying to get Turnitin assignment ID.")
							Thread.sleep(WaitingRequestsFromTurnitinCallbackSleep)
							awaitTurnitinId(assignment, retries - 1)
					}
				}
		}

		private def logPrefixWithJobId: String = s"Job:${job.id} - "
	}
}