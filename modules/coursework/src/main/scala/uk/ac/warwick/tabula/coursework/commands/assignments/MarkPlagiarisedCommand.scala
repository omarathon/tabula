package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import collection.JavaConversions._
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.tabula.data.model.PlagiarismInvestigation.SuspectPlagiarised

class MarkPlagiarisedCommand(val module: Module, val assignment: Assignment) extends Command[Unit] with SelfValidating {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.ManagePlagiarismStatus, assignment)

	var submissionService = Wire.auto[SubmissionService]

	var students: JList[String] = JArrayList()
	var confirm: Boolean = false
	var markPlagiarised: Boolean = true

	var submissionsUpdated = 0

	def applyInternal() {
		for (uniId <- students; submission <- submissionService.getSubmissionByUniId(assignment, uniId)) {
			submission.plagiarismInvestigation = SuspectPlagiarised
			submissionService.saveSubmission(submission)
			submissionsUpdated = submissionsUpdated + 1
		}
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "submission.mark.plagiarised.confirm")
	}

	override def describe(d: Description) = d
		.assignment(assignment)
		.property("students" -> students)


	override def describeResult(d: Description) = d
		.assignment(assignment)
		.property("submissionCount" -> submissionsUpdated)
		.property("markPlagiarised" -> markPlagiarised)
}
