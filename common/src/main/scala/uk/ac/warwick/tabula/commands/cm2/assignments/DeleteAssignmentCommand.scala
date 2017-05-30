package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.{Command, Description, SchedulesNotifications, SelfValidating}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.permissions._

class DeleteAssignmentCommand(val assignment: Assignment = null)
	extends Command[Assignment] with SelfValidating with SchedulesNotifications[Assignment, Assignment] {

	PermissionCheck(Permissions.Assignment.Delete, assignment)

	var confirm: JBoolean = false

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
		} else if (assignment.hasReleasedFeedback) {
			errors.reject("assignment.delete.hasFeedback")
		}
	}

	/**
		* Test whether we could delete this assignment.
		*/
	def prechecks(errors: Errors) {
		commonChecks(errors)
	}

	override def applyInternal(): Assignment = transactional() {
		assignment.markDeleted()
		assignment
	}

	override def describe(d: Description): Unit = d.assignment(assignment)


	override def transformResult(assignment: Assignment) = Seq(assignment)

	//TAB-2725 - no notification required for entities marked as delete
	override def scheduledNotifications(assignment: Assignment) = Seq()

}