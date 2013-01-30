package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.CurrentUser
import scala.annotation.tailrec

abstract class Role {

	private var permissions: Map[Permission, Option[PermissionsTarget]] = Map()
	private var roles: Set[Role] = Set()
	
	def explicitPermissions = permissions
	def subRoles = roles
		
	def GrantsPermission(scopelessPermissions: ScopelessPermission*) = grant(None, scopelessPermissions)
	def GrantsGlobalPermission(globalPermissions: Permission*) = grant(None, globalPermissions)
	
	def GrantsPermissionFor(scope: => PermissionsTarget, permissionsForScope: Permission*) = grant(Option(scope), permissionsForScope)
			
	private def grant(scope: => Option[PermissionsTarget], perms: Iterable[Permission]): Unit =
		for (permission <- perms)
			permissions += (permission -> scope)
	
	def GrantsRole(role: Role) = roles += role
	
}

abstract class BuiltInRole extends Role {}