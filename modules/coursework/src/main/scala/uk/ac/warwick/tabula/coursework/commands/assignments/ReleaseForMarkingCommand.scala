package uk.ac.warwick.tabula.coursework.commands.assignments

import collection.JavaConversions._
import reflect.BeanProperty
import uk.ac.warwick.tabula.data.model.{Received, ReleasedForMarking, Submission, Assignment}
import uk.ac.warwick.tabula.commands.{SelfValidating, Description, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{SubmissionService, AssignmentService}
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.data.model.SubmissionState._
import org.springframework.validation.Errors

class ReleaseForMarkingCommand(val assignment: Assignment) extends Command[Unit] with SelfValidating {
	var assignmentService = Wire.auto[AssignmentService]
	var submissionService = Wire.auto[SubmissionService]

	@BeanProperty var students: JList[String] = ArrayList()
	@BeanProperty var confirm: Boolean = false
	@BeanProperty var invalidSubmissions: JList[Submission] = ArrayList()

	var submissionsUpdated = 0

	def applyInternal() {
		val submissions = for (
			uniId <- students;
			submission <- assignmentService.getSubmissionByUniId(assignment, uniId)
		) yield submission

		val submissionsToUpdate = submissions -- invalidSubmissions
		submissionsToUpdate foreach (submissionService.updateState(_, ReleasedForMarking))

		submissionsUpdated = submissionsToUpdate.size
	}

	override def describe(d: Description){
		d.assignment(assignment)
		.property("students" -> students)
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
		.property("submissionCount" -> submissionsUpdated)
	}

	def preSubmitValidation() {
		invalidSubmissions = for {
			uniId <- students
			submission <- assignmentService.getSubmissionByUniId(assignment, uniId)
			if (submission.state != Received)
		} yield submission
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "submission.mark.plagiarised.confirm")
	}

}
