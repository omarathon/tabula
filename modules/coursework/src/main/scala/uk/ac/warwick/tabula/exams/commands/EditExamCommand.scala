package uk.ac.warwick.tabula.exams.commands

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Exam
import uk.ac.warwick.tabula.helpers.StringUtils._
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
			with PopulateEditExamCommandInternal
			with EditExamValidation
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

trait EditExamCommandState {

	def exam: Exam

	var name: String = _
}

trait EditExamCommandDescription extends Describable[Exam] {

	self: EditExamCommandState =>

	def describe(d: Description) {
		d.exam(exam)
	}
}

trait EditExamValidation extends SelfValidating {

	self: EditExamCommandState =>

	override def validate(errors: Errors) {

		if (!name.hasText) {
			errors.rejectValue("name", "exam.name.empty")
		}
	}
}

trait PopulateEditExamCommandInternal extends PopulateOnForm {

	self: EditExamCommandState =>

	override def populate() = {
		name = exam.name
	}
}
