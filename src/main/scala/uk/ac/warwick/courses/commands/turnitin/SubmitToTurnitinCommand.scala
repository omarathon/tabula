package uk.ac.warwick.courses.commands.turnitin

import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.services.turnitin._

/** Submit a submission to anassignment */
class SubmitToTurnitinCommand(assignment:Assignment, submissions:Seq[Submission]) extends AbstractTurnitinCommand[Unit] {
	
	def apply = {
		val assignmentId = createOrGetAssignment(assignment)
		logger.debug("Assignment ID: " + assignmentId.getOrElse("UNKNOWN!!"))
		assignmentId map { id =>
			// get a list of already-submitted items here and either ignore them or delete them
			
			submissions foreach { submission =>
				submission.allAttachments foreach { attachment => 
					api.submitPaper(classNameFor(assignment), assignment.id, attachment.name, attachment.file, submission.universityId, submission.userId)
				}
			}
		}
	}
	
	def describe(d:Description) = d
			.assignment(assignment)
			.submissions(submissions)
}