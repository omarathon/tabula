package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.ScopelessPermission
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget

object RoleBuilder {
	def build(definition: RoleDefinition, scope: Option[PermissionsTarget], name: String) = {
		new GeneratedRole(scope, name).applyRoleDefinition(definition)
	}
	
	class GeneratedRole(scope: Option[PermissionsTarget], val name: String) extends Role(scope) {
		override def getName = name
	}
}