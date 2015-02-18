package uk.ac.warwick.tabula.exams.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ExamServiceComponent, AutowiringExamServiceComponent}
import org.springframework.validation.Errors

object AddExamCommand  {
	def apply(module: Module) =
		new AddExamCommandInternal(module)
			with ComposableCommand[Exam]
			with AddExamPermissions
			with AddExamCommandState
			with AddExamCommandDescription
			with AddExamValidation
			with AutowiringExamServiceComponent
			with CurrentSITSAcademicYear

}

class AddExamCommandInternal(val module: Module) extends CommandInternal[Exam] with AddExamCommandState {

	self: ExamServiceComponent with CurrentSITSAcademicYear =>

	override def applyInternal() = {
		val exam = new Exam
		exam.name = name
		exam.academicYear = academicYear
		examService.saveOrUpdate(exam)
		exam
	}
}


trait AddExamPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AddExamCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.Manage, module.adminDepartment)
	}

}

trait AddExamCommandState {
	def module: Module

	// bind variables
	var name: String = _
}

trait AddExamCommandDescription extends Describable[Exam] {
	self: AddExamCommandState =>

	def describe(d: Description) {
		d.module(module)
	}
}

trait AddExamValidation extends SelfValidating {

	self: AddExamCommandState =>

	override def validate(errors: Errors) {
		if (name == null) {
			errors.rejectValue("name", "exam.name.empty")
		}
	}

}
