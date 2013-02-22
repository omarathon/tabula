package uk.ac.warwick.tabula.data

import org.hibernate.criterion._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.permissions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.permissions.Permission

trait PermissionsDao {
	def saveOrUpdate(roleDefinition: CustomRoleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_])
	def saveOrUpdate(role: GrantedRole[_])
	
	def getGrantedRolesFor[A <: PermissionsTarget : Manifest](scope: => A): Seq[GrantedRole[A]]
	def getGrantedPermissionsFor[A <: PermissionsTarget : Manifest](scope: => A): Seq[GrantedPermission[A]]
	
	def getGrantedRole[A <: PermissionsTarget : Manifest](scope: => A, customRoleDefinition: CustomRoleDefinition): Option[GrantedRole[A]]
	def getGrantedRole[A <: PermissionsTarget : Manifest](scope: => A, builtInRoleDefinition: BuiltInRoleDefinition): Option[GrantedRole[A]]
	
	def getGrantedPermission[A <: PermissionsTarget : Manifest](scope: => A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]]
}

@Repository
class PermissionsDaoImpl extends PermissionsDao with Daoisms {
	import Restrictions._
	import Order._
	
	def saveOrUpdate(roleDefinition: CustomRoleDefinition) = session.saveOrUpdate(roleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_]) = session.saveOrUpdate(permission)
	def saveOrUpdate(role: GrantedRole[_]) = session.saveOrUpdate(role)
	
	def getGrantedRolesFor[A <: PermissionsTarget : Manifest](scope: => A) =
		session.newCriteria[GrantedRole[A]]
					 .add(is("scope", scope))
					 .seq
	
	def getGrantedPermissionsFor[A <: PermissionsTarget : Manifest](scope: => A) =
		session.newCriteria[GrantedPermission[A]]
					 .add(is("scope", scope))
					 .seq
					 
	def getGrantedRole[A <: PermissionsTarget : Manifest](scope: => A, customRoleDefinition: CustomRoleDefinition) = 
		session.newCriteria[GrantedRole[A]]
					 .add(is("scope", scope))
					 .add(is("customRoleDefinition", customRoleDefinition))
					 .seq.headOption
					 
	def getGrantedRole[A <: PermissionsTarget : Manifest](scope: => A, builtInRoleDefinition: BuiltInRoleDefinition) = 
		session.newCriteria[GrantedRole[A]]
					 .add(is("scope", scope))
					 .add(is("builtInRoleDefinition", builtInRoleDefinition))
					 .seq.headOption
					 
	def getGrantedPermission[A <: PermissionsTarget : Manifest](scope: => A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]] =
		session.newCriteria[GrantedPermission[A]]
					 .add(is("scope", scope))
					 .add(is("permission", permission))
					 .add(is("overrideType", overrideType))
					 .seq.headOption
					
}
