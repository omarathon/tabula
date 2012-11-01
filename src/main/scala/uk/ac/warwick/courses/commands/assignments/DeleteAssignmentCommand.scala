package uk.ac.warwick.courses.commands.assignments

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils
import scala.reflect.BeanProperty

class DeleteAssignmentCommand(val assignment: Assignment = null) extends Command[Unit] {

	@BeanProperty var confirm: JBoolean = false

	def validate(errors: Errors) {
		if (!confirm) {
			errors.rejectValue("confirm", "assignment.delete.confirm")
		} else {
			commonChecks(errors)
		}
	}

	private def commonChecks(errors: Errors) {
		if (assignment.deleted) {
			errors.reject("assignment.delete.deleted")
		} else if (!assignment.submissions.isEmpty) {
			errors.reject("assignment.delete.hasSubmissions")
		} else if (assignment.anyReleasedFeedback) {
			errors.reject("assignment.delete.hasFeedback")
		}
	}

	/**
	 * Test whether we could delete this assignment.
	 */
	def prechecks(errors: Errors) {
		commonChecks(errors)
	}

	@Transactional
	override def work {
		assignment.markDeleted
	}

	override def describe(d: Description) = d.assignment(assignment)

}