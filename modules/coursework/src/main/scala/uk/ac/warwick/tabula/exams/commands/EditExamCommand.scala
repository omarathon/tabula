package uk.ac.warwick.tabula.exams.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Exam
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object EditExamCommand {
	def apply(exam: Exam) =
		new EditExamCommandInternal(exam)
			with ComposableCommand[Exam]
			with EditExamPermissions
			with EditExamCommandState
			with EditExamCommandDescription
			with PopulateEditExamCommand
			with ExamValidation
			with AutowiringAssessmentServiceComponent
}

class EditExamCommandInternal(val exam: Exam)
	extends CommandInternal[Exam] with EditExamCommandState {

	self: AssessmentServiceComponent =>

	override def applyInternal() = {
		exam.name = name
		assessmentService.save(exam)
		exam
	}
}

trait EditExamPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: EditExamCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, exam)
	}
}

trait EditExamCommandState extends ExamState {

	def exam: Exam

}

trait EditExamCommandDescription extends Describable[Exam] {

	self: EditExamCommandState =>

	def describe(d: Description) {
		d.exam(exam)
	}
}

trait PopulateEditExamCommand {

	self: EditExamCommandState =>
	name = exam.name
}
