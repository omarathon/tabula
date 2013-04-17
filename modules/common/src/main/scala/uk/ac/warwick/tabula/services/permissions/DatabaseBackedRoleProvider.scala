package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.data.PermissionsDao
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.roles.RoleBuilder
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.permissions.Permission

@Component
class DatabaseBackedRoleProvider extends RoleProvider with PermissionsProvider {
	
	var service = Wire[PermissionsService]
	
	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[Role] = 
		service.getGrantedRolesFor(user, scope) map { _.build() }
	
	def rolesProvided = Set(classOf[RoleBuilder.GeneratedRole])
	
	def getPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Stream[PermissionDefinition] =
		service.getGrantedPermissionsFor(user, scope).toStream map { 
			grantedPermission => PermissionDefinition(grantedPermission.permission, Some(scope), grantedPermission.overrideType)
		}
	
	// This role provider is intended to be exhaustive; continue to iterate up the permissions hierarchy
	override def isExhaustive = true

}