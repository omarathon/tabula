package uk.ac.warwick.courses.commands.assignments

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.validation.Errors

class DeleteAssignmentCommand(val assignment:Assignment=null) extends Command[Unit] {
	
	def validate(errors:Errors) {
		if (assignment.deleted) {
			errors.reject("assignment.delete.deleted")
		} else if (!assignment.submissions.isEmpty) {
			errors.reject("assignment.delete.hasSubmissions")
		} else if (assignment.anyReleasedFeedback) {
			errors.reject("assignment.delete.hasFeedback")
		}
	}
	
	@Transactional
	override def apply {
	  assignment.markDeleted
	}
	
	override def describe(d:Description) = d.assignment(assignment)
	
}