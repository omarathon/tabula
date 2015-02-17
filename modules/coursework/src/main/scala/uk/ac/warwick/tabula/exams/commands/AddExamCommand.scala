package uk.ac.warwick.tabula.exams.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions._

object AddExamCommand  {
	def apply(module: Module) =
		new AddExamCommandInternal(module)
			with AddExamPermissions
			with AddExamCommandState
			with ComposableCommand[Module]
			/// change this!!!
			with Unaudited

}

class AddExamCommandInternal(val module: Module)
	extends CommandInternal[Module] {

	override def applyInternal() = {
		module
	}
}


trait AddExamPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AddExamCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Module.Administer, module)
	}

}

trait AddExamCommandState {
	def module: Module
}
