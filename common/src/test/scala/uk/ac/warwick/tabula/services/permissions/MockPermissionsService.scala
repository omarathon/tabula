package uk.ac.warwick.tabula.services.permissions
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Department, UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, GrantedPermission, GrantedRole}
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget}
import uk.ac.warwick.tabula.roles.RoleDefinition

import scala.reflect.ClassTag

class MockPermissionsService extends PermissionsService {
  override def saveOrUpdate(roleDefinition: CustomRoleDefinition): Unit = {}
  override def saveOrUpdate(permission: GrantedPermission[_]): Unit = {}
  override def saveOrUpdate(role: GrantedRole[_]): Unit = {}
  override def delete(role: GrantedRole[_]): Unit = {}
  override def delete(roleDefinition: CustomRoleDefinition): Unit = {}

  override def ensureUserGroupFor[A <: PermissionsTarget : ClassTag](scope: A, roleDefinition: RoleDefinition): UnspecifiedTypeUserGroup = UserGroup.ofUsercodes

  override def clearCachesForUser(cacheKey: (String, ClassTag[_ <: PermissionsTarget]), propagate: Boolean): Unit = {}
  override def clearCachesForWebgroups(cacheKey: (Seq[String], ClassTag[_ <: PermissionsTarget]), propagate: Boolean): Unit = {}

  override def getCustomRoleDefinitionById(id: String): Option[CustomRoleDefinition] = ???
  override def getOrCreateGrantedRole[A <: PermissionsTarget : ClassTag](target: A, defn: RoleDefinition): GrantedRole[A] = ???
  override def getGrantedRole[A <: PermissionsTarget : ClassTag](scope: A, roleDefinition: RoleDefinition): Option[GrantedRole[A]] = ???
  override def getGrantedPermission[A <: PermissionsTarget : ClassTag](scope: A, permission: Permission, overrideType: Boolean): Option[GrantedPermission[A]] = ???
  override def getGrantedRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedRole[_]] = ???
  override def getGrantedPermissionsFor(user: CurrentUser, scope: PermissionsTarget): Seq[GrantedPermission[_]] = ???
  override def getAllGrantedRolesFor(user: CurrentUser): Seq[GrantedRole[_]] = ???
  override def getAllGrantedPermissionsFor(user: CurrentUser): Seq[GrantedPermission[_]] = ???
  override def getAllGrantedRolesFor[A <: PermissionsTarget : ClassTag](scope: A): Seq[GrantedRole[A]] = ???
  override def getAllGrantedPermissionsFor[A <: PermissionsTarget : ClassTag](scope: A): Seq[GrantedPermission[A]] = ???
  override def getAllGrantedRolesForDefinition(roleDefinition: RoleDefinition): Seq[GrantedRole[_]] = ???
  override def getGrantedRolesFor[A <: PermissionsTarget : ClassTag](user: CurrentUser): LazyList[GrantedRole[A]] = ???
  override def getGrantedPermissionsFor[A <: PermissionsTarget : ClassTag](user: CurrentUser): LazyList[GrantedPermission[A]] = ???
  override def getAllPermissionDefinitionsFor[A <: PermissionsTarget : ClassTag](user: CurrentUser, targetPermission: Permission): Set[A] = ???
  override def getCustomRoleDefinitionsBasedOn(roleDefinition: RoleDefinition): Seq[CustomRoleDefinition] = ???
  override def getCustomRoleDefinitionsFor(department: Department): Seq[CustomRoleDefinition] = ???
}
