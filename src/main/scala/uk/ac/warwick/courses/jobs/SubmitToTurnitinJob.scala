package uk.ac.warwick.courses.jobs

import collection.mutable.StringBuilder
import collection.JavaConversions._
import uk.ac.warwick.courses.commands.turnitin._
import uk.ac.warwick.courses.services.turnitin._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.helpers._
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.services.jobs.JobInstance
import org.springframework.stereotype.Component
import java.io.PrintWriter
import java.io.StringWriter
import scala.annotation.tailrec
import uk.ac.warwick.util.mail.WarwickMailSender
import javax.annotation.Resource
import uk.ac.warwick.courses.commands.Describable
import uk.ac.warwick.courses.commands.Description

object SubmitToTurnitinJob {
	val identifier = "turnitin-submit"
	def apply(assignment:Assignment) = JobPrototype(identifier, Map(
		"assignment"->assignment.id
	))
}

abstract class DescribableJob(instance:JobInstance) extends Describable[Nothing] {
	val eventName:String = instance.getClass.getSimpleName.replaceAll("Job$","")
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
class SubmitToTurnitinJob extends Job with TurnitinTrait with Logging {

	val identifier = SubmitToTurnitinJob.identifier
	
	@Autowired var assignmentService: AssignmentService = _
	@Resource(name="mailSender") var mailer: WarwickMailSender = _
	
	def run(implicit job:JobInstance) {
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
	
	def describable(instance:JobInstance) = new DescribableJob(instance) {
		def describe(description: Description) {
			
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
	
	def uploadSubmissions(assignment: Assignment, existingSubmissions: Seq[TurnitinSubmissionInfo]) (implicit job:JobInstance) {
		var uploadsDone = 0
		val allAttachments = assignment.submissions flatMap { _.allAttachments }
		val uploadsTotal = allAttachments.size
		assignment.submissions foreach { submission =>
			if (debugEnabled) logger.debug("Submission ID: " + submission.id)
			logger.debug("submission.allAttachments: ("+submission.allAttachments.size+")")
			submission.allAttachments foreach { attachment =>
				val alreadyUploaded = existingSubmissions.exists( info => info.universityId == submission.universityId && info.title == attachment.name )
				if (alreadyUploaded) {
					// we don't need to upload it again, probably
					if (debugEnabled) logger.debug("Not uploading attachment again because it looks like it's been uploaded before: " + attachment.name + "(by "+submission.universityId+")")
				} else {
					if (debugEnabled) logger.debug("Uploading attachment: " + attachment.name + " (by "+submission.universityId+")")
					val submitResponse = api.submitPaper(classNameFor(assignment), assignmentNameFor(assignment), attachment.name, attachment.file, submission.universityId, "Student")
					if (debugEnabled) logger.debug("submitResponse: " + submitResponse)
					if (!submitResponse.successful) {
						logger.debug("Failed to upload document " + attachment.name)
					}
				}
				uploadsDone += 1
			}
			progress = (10 + ((uploadsDone*80) / uploadsTotal)) // 10% - 90%
		}
		
		logger.debug("Done uploads ("+uploadsDone+")")
	}
	
	def retrieveReport(turnitinId: String, assignment: Assignment) (implicit job:JobInstance) {
		// Uploaded all the submissions probably, now we wait til they're all checked
		status = "Waiting for documents to be checked..."
			
		// try 50 times with a 10 sec sleep inbetween until we get a full Turnitin report.
		val submissions = runCheck(assignment, 50) getOrElse Nil
		
		if (submissions.isEmpty) {
			logger.error("Waited for complete Turnitin report but didn't get one. Turnitin assignment: "+turnitinId)
			status = "Failed to generate a report. "
		} else {
			
			// TODO persist report
			// TODO send an email
			
			val s = new StringBuilder
			s append "Originality report for " append assignment.name
			s append " (" append assignment.module.code.toUpperCase append ")\n"
			
			for (submission <- submissions) {
				s append "---\n"
				s append submission.universityId append "\n"
				s append " Similarity=" append submission.similarityScore append "\n"
				s append " Overlap=" append submission.overlap append "\n"
				s append " Student Overlap=" append submission.studentPaperOverlap append "\n"
				s append " Web Overlap=" append submission.webOverlap append "\n"
				s append " Publication Overlap=" append submission.publicationOverlap append "\n"
			}
			
			logger.info("Here is a report.")
			logger.info(s.toString)
			
			status = "Generated a report."
			progress = 100
			job.succeeded = true
		}
	}
	
	/** Recursive function to repeatedly check if all the documents have been scored yet.
	 * If we run out of attempts we return None. Otherwise we return Some(list), so you can
	 * distinguish between a timeout and an actual empty collection.
	 */
	def runCheck(assignment:Assignment, retries:Int)(implicit job:JobInstance) : Option[Seq[TurnitinSubmissionInfo]] = {
		if (retries == 0) None
		else runCheck(assignment) orElse runCheck(assignment, retries - 1)
	}
	
	def runCheck(assignment:Assignment)(implicit job:JobInstance) : Option[Seq[TurnitinSubmissionInfo]] = {
		Thread.sleep(10000)
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