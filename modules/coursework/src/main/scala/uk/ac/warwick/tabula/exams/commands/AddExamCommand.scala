package uk.ac.warwick.tabula.exams.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions

object AddExamCommand  {
	def apply(amodule: Module) =
		new AddExamCommandInternal(amodule)
			with ComposableCommand[Module]
			with AddExamPermissions
			with AddExamCommandState
			with AddExamCommandDescription
}

class AddExamCommandInternal(val module: Module) extends CommandInternal[Module] with AddExamCommandState {

	override def applyInternal() = {
		module
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
