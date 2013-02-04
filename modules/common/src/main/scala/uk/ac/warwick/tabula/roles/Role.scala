package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.CurrentUser
import scala.annotation.tailrec
import scala.reflect.BeanProperty
import scala.collection.mutable.LinkedHashMap

abstract class Role(@BeanProperty val scope: Option[PermissionsTarget]) {

	private var permissions: LinkedHashMap[Permission, Option[PermissionsTarget]] = LinkedHashMap()
	private var roles: Set[Role] = Set()
	
	def getName = getClass.getSimpleName
	def isScoped = scope.isDefined
	
	def explicitPermissions = permissions
	def explicitPermissionsAsList = permissions.toList
	def subRoles = roles
		
	def GrantsPermission(scopelessPermissions: ScopelessPermission*) = grant(None, scopelessPermissions)
	def GrantsGlobalPermission(globalPermissions: Permission*) = grant(None, globalPermissions)
	
	def GrantsPermissionFor(scope: => PermissionsTarget, permissionsForScope: Permission*) = grant(Option(scope), permissionsForScope)
			
	private def grant(scope: => Option[PermissionsTarget], perms: Iterable[Permission]): Unit =
		for (permission <- perms)
			permissions += (permission -> scope)
	
	def GrantsRole(role: Role) = roles += role
	
}

abstract class BuiltInRole(scope: Option[PermissionsTarget]) extends Role(scope) {
	def this(scope: PermissionsTarget) {
		this(Option(scope))
	}
}