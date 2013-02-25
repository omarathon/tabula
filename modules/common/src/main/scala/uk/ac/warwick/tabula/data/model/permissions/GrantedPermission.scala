package uk.ac.warwick.tabula.data.model.permissions

import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.data.model.HibernateVersioned
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import org.hibernate.annotations.ForeignKey

@Entity
@AccessType("field")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
		name="scope_type",
		discriminatorType=DiscriminatorType.STRING
)
abstract class GrantedPermission[A <: PermissionsTarget] extends GeneratedId with HibernateVersioned with PostLoadBehaviour {
	import GrantedPermission.OverrideType
	
	@OneToOne(cascade=Array(CascadeType.ALL))
	@JoinColumn(name="usergroup_id")
	@BeanProperty var users: UserGroup = new UserGroup
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.PermissionUserType")
	@BeanProperty var permission: Permission = _
	
	/*
	 * TODO Deny not currently supported by SecurityService because the value isn't passed through!
	 */
	@BeanProperty var overrideType: OverrideType = _
	
	var scope: A
	
	// If hibernate sets users to null, make a new empty usergroup
	override def postLoad {
		ensureUsers
	}

	def ensureUsers = {
		if (users == null) users = new UserGroup
		users
	}

}

object GrantedPermission {
	type OverrideType = Boolean
	val Allow: OverrideType = true
	val Deny: OverrideType = false
	
	def init[A <: PermissionsTarget](scope: A, permission: Permission, overrideType: OverrideType): GrantedPermission[A] =
		(scope match {
			case dept: Department => new DepartmentGrantedPermission(dept, permission, overrideType)
			case module: Module => new ModuleGrantedPermission(module, permission, overrideType)
			case member: Member => new MemberGrantedPermission(member, permission, overrideType)
			case assignment: Assignment => new AssignmentGrantedPermission(assignment, permission, overrideType)
			case _ => throw new IllegalArgumentException("Cannot define new permissions for " + scope)
		}).asInstanceOf[GrantedPermission[A]]
	
	def canDefineFor[A <: PermissionsTarget : Manifest](scope: A) = scope match {
		case _: Department => true
		case _: Module => true
		case _: Member => true
		case _: Assignment => true
		case _ => false
	} 
}

/* Ok, this is icky, but I can't find any other way. If you need new targets for GrantedPermissions, create them below with a new discriminator */
@Entity @DiscriminatorValue("Department") class DepartmentGrantedPermission extends GrantedPermission[Department] {
	def this(department: Department, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = department
		this.permission = permission
		this.overrideType = overrideType
	}
	
	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	@BeanProperty var scope: Department = _
}
@Entity @DiscriminatorValue("Module") class ModuleGrantedPermission extends GrantedPermission[Module] {
	def this(module: Module, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = module
		this.permission = permission
		this.overrideType = overrideType
	}
	
	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	@BeanProperty var scope: Module = _
}
@Entity @DiscriminatorValue("Member") class MemberGrantedPermission extends GrantedPermission[Member] {
	def this(member: Member, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = member
		this.permission = permission
		this.overrideType = overrideType
	}
	
	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	@BeanProperty var scope: Member = _
}
@Entity @DiscriminatorValue("Assignment") class AssignmentGrantedPermission extends GrantedPermission[Assignment] {
	def this(assignment: Assignment, permission: Permission, overrideType: GrantedPermission.OverrideType) = {
		this()
		this.scope = assignment
		this.permission = permission
		this.overrideType = overrideType
	}
	
	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	@BeanProperty var scope: Assignment = _
}