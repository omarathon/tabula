package uk.ac.warwick.tabula.exams.commands

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object EditExamCommand {
	def apply(module: Module, academicYear: AcademicYear, exam: Exam) =
		new EditExamCommandInternal(module, academicYear, exam)
			with ComposableCommand[Exam]
			with EditExamPermissions
			with EditExamCommandState
			with EditExamCommandDescription
			with PopulateEditExamCommandInternal
			with EditExamValidation
			with AutowiringAssessmentServiceComponent
}

class EditExamCommandInternal(val module: Module, val academicYear: AcademicYear, val exam: Exam)
	extends CommandInternal[Exam] with EditExamCommandState {

	self: AssessmentServiceComponent =>

	override def applyInternal() = {
		exam.name = name
		exam.module = module
		exam.academicYear = academicYear
		assessmentService.save(exam)
		exam
	}
}

trait EditExamPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: EditExamCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Create, module)
	}

}

trait EditExamCommandState {
	def module: Module
	def academicYear: AcademicYear
	def exam: Exam

	// bind variables
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
