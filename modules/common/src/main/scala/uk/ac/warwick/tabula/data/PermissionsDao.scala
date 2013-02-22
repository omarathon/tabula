package uk.ac.warwick.tabula.data

import org.hibernate.criterion._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.permissions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition

trait PermissionsDao {
	def saveOrUpdate(roleDefinition: CustomRoleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_])
	def saveOrUpdate(role: GrantedRole[_])
	
	def getGrantedRolesFor(scope: => PermissionsTarget): Seq[GrantedRole[_]]
	def getGrantedPermissionsFor(scope: => PermissionsTarget): Seq[GrantedPermission[_]]
	
	def getGrantedRole(scope: => PermissionsTarget, customRoleDefinition: CustomRoleDefinition): Option[GrantedRole[_]]
	def getGrantedRole(scope: => PermissionsTarget, builtInRoleDefinition: BuiltInRoleDefinition): Option[GrantedRole[_]]
}

@Repository
class PermissionsDaoImpl extends PermissionsDao with Daoisms {
	import Restrictions._
	import Order._
	
	def saveOrUpdate(roleDefinition: CustomRoleDefinition) = session.saveOrUpdate(roleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_]) = session.saveOrUpdate(permission)
	def saveOrUpdate(role: GrantedRole[_]) = session.saveOrUpdate(role)
	
	def getGrantedRolesFor(scope: => PermissionsTarget) =
		session.newCriteria[GrantedRole[_]]
					 .add(is("scope", scope))
					 .seq
	
	def getGrantedPermissionsFor(scope: => PermissionsTarget) =
		session.newCriteria[GrantedPermission[_]]
					 .add(is("scope", scope))
					 .seq
					 
	def getGrantedRole(scope: => PermissionsTarget, customRoleDefinition: CustomRoleDefinition) = 
		session.newCriteria[GrantedRole[_]]
					 .add(is("scope", scope))
					 .add(is("customRoleDefinition", customRoleDefinition))
					 .seq.headOption
					 
	def getGrantedRole(scope: => PermissionsTarget, builtInRoleDefinition: BuiltInRoleDefinition) = 
		session.newCriteria[GrantedRole[_]]
					 .add(is("scope", scope))
					 .add(is("builtInRoleDefinition", builtInRoleDefinition))
					 .seq.headOption
					
}
