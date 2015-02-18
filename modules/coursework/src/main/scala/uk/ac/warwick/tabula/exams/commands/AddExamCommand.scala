package uk.ac.warwick.tabula.exams.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ExamServiceComponent, AutowiringExamServiceComponent}

object AddExamCommand  {
	def apply(module: Module) =
		new AddExamCommandInternal(module)
			with ComposableCommand[Exam]
			with AddExamPermissions
			with AddExamCommandState
			with AddExamCommandDescription
			with AutowiringExamServiceComponent
}

class AddExamCommandInternal(val module: Module) extends CommandInternal[Exam] with AddExamCommandState {

	self: ExamServiceComponent =>

	override def applyInternal() = {
		val exam = new Exam
		examService.saveOrUpdate(exam)
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
}

trait AddExamCommandDescription extends Describable[Module] {
	self: AddExamCommandState =>

	def describe(d: Description) {
		d.module(module)
	}
}
