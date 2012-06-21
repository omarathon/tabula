package uk.ac.warwick.courses.jobs

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

object SubmitToTurnitinJob {
	val identifier = "turnitin-submit"
	def apply(assignment:Assignment) = JobPrototype(identifier, Map(
		"assignment"->assignment.id
	))
}

@Component
class SubmitToTurnitinJob extends Job with TurnitinTrait with Logging {

	val identifier = SubmitToTurnitinJob.identifier
	
	@Autowired var assignmentService:AssignmentService =_
	
	def run(implicit job:JobInstance) {
		status = ("Submitting to Turnitin")
		val id = job.getString("assignment")
		val assignment = assignmentService.getAssignmentById(id) getOrElse (throw obsoleteJob)
		val assignmentId = createOrGetAssignment(assignment)
		logger.debug("Assignment ID: " + assignmentId.getOrElse("UNKNOWN!!"))
		assignmentId map { id =>
			// get a list of already-submitted items here and either ignore them or delete them
			val response = api.listSubmissions(classNameFor(assignment), assignment.id)
			val existingSubmissions = response match {
				case GotSubmissions(list) => list
				case _ => Nil
			}
			
			println(existingSubmissions)
			
//			submissions foreach { submission =>
//				logger.debug("Submission ID: " + submission.id)
//				submission.allAttachments foreach { attachment => 
//					api.submitPaper(classNameFor(assignment), assignment.id, attachment.name, attachment.file, submission.universityId, "Student")
//				}
//			}
		}
	}


}