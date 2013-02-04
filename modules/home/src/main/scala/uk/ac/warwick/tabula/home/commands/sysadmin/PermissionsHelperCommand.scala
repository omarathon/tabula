package uk.ac.warwick.tabula.home.commands.sysadmin

import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Command
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.permissions.RoleService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.permissions.Permissions._

class PermissionsHelperCommand extends Command[PermissionHelperResult] with Unaudited with ReadOnly {
	
	PermissionCheck(PermissionsHelper)
	
	var securityService = Wire.auto[SecurityService]
	var roleService = Wire.auto[RoleService]
	
	@BeanProperty var user: User = _
	@BeanProperty var scopeType: Class[_ <: PermissionsTarget] = _
	@BeanProperty var scope: String = _
	@BeanProperty var permission: Permission = _

	def applyInternal() = {
		val currentUser = new CurrentUser(user, user)
		
		val permissions = roleService.getExplicitPermissionsFor(currentUser, null)
		val roles = roleService.getRolesFor(currentUser, null)
		
		val canDo = securityService.can(currentUser, permission, null)
		
		PermissionHelperResult(
			canDo = canDo,
			permissions = permissions.toList,
			roles = roles.toList
		)
	}
	
}

case class PermissionHelperResult(
	canDo: Boolean,
	permissions: List[(Permission, Option[PermissionsTarget])],
	roles: List[Role]
)