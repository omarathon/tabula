package uk.ac.warwick.tabula.admin.commands.department

import uk.ac.warwick.tabula.commands.{CommandInternal, ReadOnly, Unaudited, ComposableCommand}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import ListCustomRolesCommand._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}

object ListCustomRolesCommand {
	case class CustomRoleInfo(customRoleDefinition: CustomRoleDefinition, grantedRoles: Int, derivedRoles: Int)

	def apply(department: Department) =
		new ListCustomRolesCommandInternal(department)
			with ComposableCommand[Seq[CustomRoleInfo]]
			with AutowiringPermissionsServiceComponent
			with ListCustomRolesCommandPermissions
			with ReadOnly with Unaudited
}

class ListCustomRolesCommandInternal(val department: Department) extends CommandInternal[Seq[CustomRoleInfo]] with ListCustomRolesCommandState {
	self: PermissionsServiceComponent =>

	def applyInternal = {
		permissionsService.getCustomRoleDefinitionsFor(department).map { defn =>
			val granted = permissionsService.getAllGrantedRolesForDefinition(defn)
			val derived = permissionsService.getCustomRoleDefinitionsBasedOn(defn)

			CustomRoleInfo(defn, granted.size, derived.size)
		}
	}

}

trait ListCustomRolesCommandPermissions extends RequiresPermissionsChecking {
	self: ListCustomRolesCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.RolesAndPermissions.Create, department)
	}
}

trait ListCustomRolesCommandState {
	def department: Department
}