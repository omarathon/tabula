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
	
	var dao = Wire.auto[PermissionsDao]
	
	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget): Seq[Role] = 
		dao.getGrantedRolesFor(scope) filter { _.users.includes(user.apparentId) } map { _.build() }
	
	def rolesProvided = Set(classOf[RoleBuilder.GeneratedRole])
	
	def getPermissionsFor(user: CurrentUser, scope: => PermissionsTarget): Stream[(Permission, Option[PermissionsTarget], Boolean)] =
		dao.getGrantedPermissionsFor(scope).toStream filter { _.users.includes(user.apparentId) } map { 
			grantedPermission => (grantedPermission.permission, Some(scope), grantedPermission.overrideType)
		}
	
	// This role provider is intended to be exhaustive; continue to iterate up the permissions hierarchy
	override def isExhaustive = true

}