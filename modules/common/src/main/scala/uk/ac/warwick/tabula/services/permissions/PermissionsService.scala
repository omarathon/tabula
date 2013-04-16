package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.PermissionsDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.permissions.Permission
import scala.reflect.ClassTag
import uk.ac.warwick.userlookup.GroupService
import scala.collection.JavaConverters._

trait PermissionsService {
	def saveOrUpdate(roleDefinition: CustomRoleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_])
	def saveOrUpdate(role: GrantedRole[_])
	
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): Option[GrantedRole[A]]
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]]
	
	def getGrantedRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedRole[_]]
	def getGrantedPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedPermission[_]]
	
	def getAllGrantedRolesFor(user: CurrentUser): Seq[GrantedRole[_]]
	def getAllGrantedPermissionsFor(user: CurrentUser): Seq[GrantedPermission[_]]
	
	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Seq[GrantedRole[A]]
	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Seq[GrantedPermission[A]]
	
	def ensureUserGroupFor[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): UserGroup
}

@Service(value = "permissionsService")
class PermissionsServiceImpl extends PermissionsService with Logging {
	
	var dao = Wire.auto[PermissionsDao]
	var groupService = Wire.auto[GroupService]
	
	def saveOrUpdate(roleDefinition: CustomRoleDefinition) = dao.saveOrUpdate(roleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_]) = dao.saveOrUpdate(permission)
	def saveOrUpdate(role: GrantedRole[_]) = dao.saveOrUpdate(role)
	
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): Option[GrantedRole[A]] = 
		transactional(readOnly = true) {
			roleDefinition match {
				case builtIn: BuiltInRoleDefinition => dao.getGrantedRole(scope, builtIn)
				case custom: CustomRoleDefinition => dao.getGrantedRole(scope, custom)
				case _ => None
			}
		}
	
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]] =
		transactional(readOnly = true) {
			dao.getGrantedPermission(scope, permission, overrideType)
		}
	
	def getGrantedRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedRole[_]] = transactional(readOnly = true) {
		dao.getGrantedRolesFor(scope) filter { _.users.includes(user.apparentId) }
	}
	
	def getGrantedPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedPermission[_]] = transactional(readOnly = true) {
		dao.getGrantedPermissionsFor(scope).toStream filter { _.users.includes(user.apparentId) }
	}
	
	def getAllGrantedRolesFor(user: CurrentUser): Seq[GrantedRole[_]] = getGrantedRolesFor[PermissionsTarget](user)
	
	def getAllGrantedPermissionsFor(user: CurrentUser): Seq[GrantedPermission[_]] = getGrantedPermissionsFor[PermissionsTarget](user)
	
	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Seq[GrantedRole[A]] = transactional(readOnly = true) {
		val groupNames = groupService.getGroupsNamesForUser(user.apparentId).asScala
		
		(
			// Get all roles where usercode is included,
			dao.getGrantedRolesForUser[A](user.apparentUser)
			
			// Get all roles backed by one of the webgroups, 		
			++ (groupNames flatMap { dao.getGrantedRolesForWebgroup[A](_) })
		)
			// For sanity's sake, filter by the users including the user
			.filter { _.users.includes(user.apparentId) }
	}
	
	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](user: CurrentUser): Seq[GrantedPermission[A]] = transactional(readOnly = true) {
		val groupNames = groupService.getGroupsNamesForUser(user.apparentId).asScala
		
		(
			// Get all permissions where usercode is included,
			dao.getGrantedPermissionsForUser[A](user.apparentUser)
			
			// Get all permissions backed by one of the webgroups, 		
			++ (groupNames flatMap { dao.getGrantedPermissionsForWebgroup[A](_) })
		)
			// For sanity's sake, filter by the users including the user
			.filter { _.users.includes(user.apparentId) }
	}
	
	def ensureUserGroupFor[A <: PermissionsTarget: ClassTag](scope: A, roleDefinition: RoleDefinition): UserGroup = transactional() {
		getGrantedRole(scope, roleDefinition) match {
			case Some(role) => role.users
			case _ => {
				val role = GrantedRole(scope, roleDefinition)
				
				dao.saveOrUpdate(role)
				role.users
			}
		}
	}
	
}