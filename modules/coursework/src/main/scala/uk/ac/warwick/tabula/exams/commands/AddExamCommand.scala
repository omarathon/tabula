package uk.ac.warwick.tabula.exams.commands

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object AddExamCommand  {
	def apply(module: Module, academicYear: AcademicYear) =
		new AddExamCommandInternal(module, academicYear)
			with ComposableCommand[Exam]
			with AddExamPermissions
			with AddExamCommandState
			with AddExamCommandDescription
			with ExamValidation
			with AutowiringAssessmentServiceComponent

}

class AddExamCommandInternal(val module: Module, val academicYear: AcademicYear) extends CommandInternal[Exam] with AddExamCommandState {

	self: AssessmentServiceComponent =>

	override def applyInternal() = {
		val exam = new Exam
		exam.name = name
		exam.module = module
		exam.academicYear = academicYear
		assessmentService.save(exam)
		exam
	}
}


trait AddExamPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AddExamCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Create, module)
	}

}

trait ExamState {
	// bind variables
	var name: String = _
}

trait AddExamCommandState extends ExamState {
	def module: Module
	def academicYear: AcademicYear
}

trait AddExamCommandDescription extends Describable[Exam] {
	self: AddExamCommandState =>

	def describe(d: Description) {
		d.module(module)
	}
}

trait ExamValidation extends SelfValidating {

	self: ExamState =>

	override def validate(errors: Errors) {

		if (!name.hasText) {
			errors.rejectValue("name", "exam.name.empty")
		}
	}

}
