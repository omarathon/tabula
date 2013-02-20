package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.ScopelessPermission
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget

object RoleBuilder {
	def build(definition: RoleDefinition, scope: Option[PermissionsTarget]) = {
		new GeneratedRole(scope).applyRoleDefinition(definition)
	}
	
	class GeneratedRole(scope: Option[PermissionsTarget]) extends Role(scope)
}