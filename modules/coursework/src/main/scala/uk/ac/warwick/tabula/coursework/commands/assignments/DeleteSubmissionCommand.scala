package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConversions._

import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.SubmissionService


class DeleteSubmissionCommand(val module: Module, val assignment: Assignment) extends Command[Unit] with SelfValidating {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Delete, assignment)

	var submissionService = Wire[SubmissionService]
	var zipService = Wire[ZipService]

	var submissions: JList[Submission] = JArrayList()
	var confirm: Boolean = false

	def applyInternal() = {
		for (submission <- submissions) submissionService.delete(submission)
		zipService.invalidateSubmissionZip(assignment)
	}

	def prevalidate(errors: Errors) {
		if (submissions.find(_.assignment != assignment).isDefined) {
			errors.reject("submission.bulk.wrongassignment")
		}
	}

	def validate(errors: Errors) {
		prevalidate(errors)
		if (!confirm) errors.rejectValue("confirm", "submission.delete.confirm")
	}

	def describe(d: Description) = 
		d.assignment(assignment)
		.property("submissionCount" -> submissions.size)

}