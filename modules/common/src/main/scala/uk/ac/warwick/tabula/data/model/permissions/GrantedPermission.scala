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
	
	@OneToOne(cascade=Array(CascadeType.ALL))
	@JoinColumn(name="usergroup_id")
	@BeanProperty var users: UserGroup = new UserGroup
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.PermissionUserType")
	@BeanProperty var permission: Permission = _
	
	type OverrideType = Boolean
	@Transient val Allow: OverrideType = true
	@Transient val Deny: OverrideType = false
	
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
	def init[A <: PermissionsTarget](implicit m: Manifest[A]): GrantedPermission[A] =
		m.erasure match {
			case _ if m.erasure == classOf[Department] => (new DepartmentGrantedPermission).asInstanceOf[GrantedPermission[A]]
			case _ if m.erasure == classOf[Module] => (new ModuleGrantedPermission).asInstanceOf[GrantedPermission[A]]
			case _ if m.erasure == classOf[Member] => (new MemberGrantedPermission).asInstanceOf[GrantedPermission[A]]
			case _ if m.erasure == classOf[Assignment] => (new AssignmentGrantedPermission).asInstanceOf[GrantedPermission[A]]
			case _ => throw new IllegalArgumentException("Cannot define new roles for " + m.erasure)
		}
}

/* Ok, this is icky, but I can't find any other way. If you need new targets for GrantedPermissions, create them below with a new discriminator */
@Entity @DiscriminatorValue("Department") class DepartmentGrantedPermission extends GrantedPermission[Department] {
	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	@BeanProperty var scope: Department = _
}
@Entity @DiscriminatorValue("Module") class ModuleGrantedPermission extends GrantedPermission[Module] {
	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	@BeanProperty var scope: Module = _
}
@Entity @DiscriminatorValue("Member") class MemberGrantedPermission extends GrantedPermission[Member] {
	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	@BeanProperty var scope: Member = _
}
@Entity @DiscriminatorValue("Assignment") class AssignmentGrantedPermission extends GrantedPermission[Assignment] {
	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	@BeanProperty var scope: Assignment = _
}