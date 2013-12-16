package uk.ac.warwick.tabula.data

import org.hibernate.criterion._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{RoleDefinition, BuiltInRoleDefinition}
import uk.ac.warwick.tabula.permissions.Permission
import scala.reflect.ClassTag
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.spring.Wire

trait PermissionsDao {
	def saveOrUpdate(roleDefinition: CustomRoleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_])
	def saveOrUpdate(role: GrantedRole[_])
	
	def getCustomRoleDefinitionById(id: String): Option[CustomRoleDefinition]
	
	def getGrantedRole[A <: PermissionsTarget: ClassTag](id: String): Option[GrantedRole[A]]
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](id: String): Option[GrantedPermission[A]]
	
	def getGrantedRolesById[A <: PermissionsTarget: ClassTag](ids: Seq[String]): Seq[GrantedRole[A]]
	def getGrantedPermissionsById[A <: PermissionsTarget: ClassTag](ids: Seq[String]): Seq[GrantedPermission[A]]
	
	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](scope: A): Seq[GrantedRole[A]]
	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](scope: A): Seq[GrantedPermission[A]]
	
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, customRoleDefinition: CustomRoleDefinition): Option[GrantedRole[A]]
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, builtInRoleDefinition: BuiltInRoleDefinition): Option[GrantedRole[A]]
	
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]]
	
	def getGrantedRolesForUser[A <: PermissionsTarget: ClassTag](user: User): Seq[GrantedRole[A]]
	def getGrantedRolesForWebgroups[A <: PermissionsTarget: ClassTag](groupNames: Seq[String]): Seq[GrantedRole[A]]
	
	def getGrantedPermissionsForUser[A <: PermissionsTarget: ClassTag](user: User): Seq[GrantedPermission[A]]
	def getGrantedPermissionsForWebgroups[A <: PermissionsTarget: ClassTag](groupNames: Seq[String]): Seq[GrantedPermission[A]]
	def getCustomRoleDefinitionsBasedOn(baseDef:BuiltInRoleDefinition):Seq[CustomRoleDefinition]

}

@Repository
class PermissionsDaoImpl extends PermissionsDao with Daoisms {
	import Restrictions._
	import Order._
	
	def saveOrUpdate(roleDefinition: CustomRoleDefinition) = session.saveOrUpdate(roleDefinition)
	def saveOrUpdate(permission: GrantedPermission[_]) = session.saveOrUpdate(permission)
	def saveOrUpdate(role: GrantedRole[_]) = session.saveOrUpdate(role)
	
	def getCustomRoleDefinitionById(id: String) = getById[CustomRoleDefinition](id)
	
	def getGrantedRole[A <: PermissionsTarget: ClassTag](id: String) = getById[GrantedRole[A]](id)
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](id: String) = getById[GrantedPermission[A]](id)
	
	def getGrantedRolesById[A <: PermissionsTarget: ClassTag](ids: Seq[String]) = 
		if (ids.isEmpty) Nil
		else session.newCriteria[GrantedRole[A]]
					 .add(in("id", ids.asJava))
					 .seq
					 
	def getGrantedPermissionsById[A <: PermissionsTarget: ClassTag](ids: Seq[String]) =
		if (ids.isEmpty) Nil
		else session.newCriteria[GrantedPermission[A]]
					 .add(in("id", ids.asJava))
					 .seq
	
	def getGrantedRolesFor[A <: PermissionsTarget: ClassTag](scope: A) = canDefineRoleSeq(scope) {
		session.newCriteria[GrantedRole[A]]
					 .add(is("scope", scope))
					 .seq
	}
	
	def getGrantedPermissionsFor[A <: PermissionsTarget: ClassTag](scope: A) = canDefinePermissionSeq(scope) {
		session.newCriteria[GrantedPermission[A]]
					 .add(is("scope", scope))
					 .seq
	}
					 
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, customRoleDefinition: CustomRoleDefinition) = canDefineRole(scope) { 
		session.newCriteria[GrantedRole[A]]
					 .add(is("scope", scope))
					 .add(is("customRoleDefinition", customRoleDefinition))
					 .seq.headOption
	}
					 
	def getGrantedRole[A <: PermissionsTarget: ClassTag](scope: A, builtInRoleDefinition: BuiltInRoleDefinition) = canDefineRole(scope) {
		session.newCriteria[GrantedRole[A]]
					 .add(is("scope", scope))
					 .add(is("builtInRoleDefinition", builtInRoleDefinition))
					 .seq.headOption
	}
					 
	def getGrantedPermission[A <: PermissionsTarget: ClassTag](scope: A, permission: Permission, overrideType: Boolean) = canDefinePermission(scope) {
		session.newCriteria[GrantedPermission[A]]
					 .add(is("scope", scope))
					 .add(is("permission", permission))
					 .add(is("overrideType", overrideType))
					 .seq.headOption
	}
					 
	private def canDefinePermissionSeq[A <: PermissionsTarget](scope: A)(f: => Seq[GrantedPermission[A]]) = {
		if (GrantedPermission.canDefineFor(scope)) f
		else Seq()
	}
					 
	private def canDefineRoleSeq[A <: PermissionsTarget](scope: A)(f: => Seq[GrantedRole[A]]) = {
		if (GrantedRole.canDefineFor(scope)) f
		else Seq()
	}
					 
	private def canDefinePermission[A <: PermissionsTarget](scope: A)(f: => Option[GrantedPermission[A]]) = {
		if (GrantedPermission.canDefineFor(scope)) f
		else None
	}
					 
	private def canDefineRole[A <: PermissionsTarget](scope: A)(f: => Option[GrantedRole[A]]) = {
		if (GrantedRole.canDefineFor(scope)) f
		else None
	}
	
	def getGrantedRolesForUser[A <: PermissionsTarget: ClassTag](user: User) =
		session.newQuery[GrantedRole[A]]("""
				select distinct r
				from """ + GrantedRole.className[A] + """ r
				where 
					(
						r.users.universityIds = false and 
						((:userId in elements(r.users.staticIncludeUsers)
						or :userId in elements(r.users.includeUsers))
						and :userId not in elements(r.users.excludeUsers))
					) or (
						r.users.universityIds = true and 
						((:universityId in elements(r.users.staticIncludeUsers)
						or :universityId in elements(r.users.includeUsers))
						and :universityId not in elements(r.users.excludeUsers))
					)
		""")
			.setString("universityId", user.getWarwickId())
			.setString("userId", user.getUserId())
			.seq
	
	def getGrantedRolesForWebgroups[A <: PermissionsTarget: ClassTag](groupNames: Seq[String]) = {
		groupNames.grouped(Daoisms.MaxInClauseCount).flatMap { names =>
			val c = session.newCriteria[GrantedRole[A]]
				.createAlias("users", "users")
				.add(in("users.baseWebgroup", names.asJava))

			GrantedRole.discriminator[A] foreach { discriminator =>
				c.add(is("class", discriminator))
			}

			c.seq
		}.toSeq
	}
	
	def getGrantedPermissionsForUser[A <: PermissionsTarget: ClassTag](user: User) =
		session.newQuery[GrantedPermission[A]]("""
				select distinct r
				from """ + GrantedPermission.className[A] + """ r
				where 
					(
						r.users.universityIds = false and 
						((:userId in elements(r.users.staticIncludeUsers)
						or :userId in elements(r.users.includeUsers))
						and :userId not in elements(r.users.excludeUsers))
					) or (
						r.users.universityIds = true and 
						((:universityId in elements(r.users.staticIncludeUsers)
						or :universityId in elements(r.users.includeUsers))
						and :universityId not in elements(r.users.excludeUsers))
					)
		""")
			.setString("universityId", user.getWarwickId())
			.setString("userId", user.getUserId())
			.seq
	
	
	def getGrantedPermissionsForWebgroups[A <: PermissionsTarget: ClassTag](groupNames: Seq[String]) = {
		groupNames.grouped(Daoisms.MaxInClauseCount).flatMap { names =>
			val c = session.newCriteria[GrantedPermission[A]]
				.createAlias("users", "users")
				.add(in("users.baseWebgroup", names.asJava))
			
			GrantedPermission.discriminator[A] map { discriminator => 
				c.add(is("class", discriminator))
			}
			
			c.seq
		}.toSeq
	}

	def getCustomRoleDefinitionsBasedOn(baseDef: BuiltInRoleDefinition): Seq[CustomRoleDefinition] = {
		session.newCriteria[CustomRoleDefinition]
		.add(is("builtInBaseRoleDefinition", baseDef))
		.seq
	}
}

trait PermissionsDaoComponent {
	var permissionsDao:PermissionsDao
}

trait AutowiringPermissionsDaoComponent extends PermissionsDaoComponent {
	var permissionsDao = Wire[PermissionsDao]
}
