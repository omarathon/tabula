package uk.ac.warwick.tabula.data.model.permissions
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
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.roles.RoleBuilder
import uk.ac.warwick.tabula.roles.RoleDefinition
import org.hibernate.annotations.ForeignKey
import scala.reflect._
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent}
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.StudentMember

@Entity
@AccessType("field")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
		name="scope_type",
		discriminatorType=DiscriminatorType.STRING
)
abstract class GrantedRole[A <: PermissionsTarget] extends GeneratedId with HibernateVersioned with PostLoadBehaviour {

	@OneToOne(cascade=Array(CascadeType.ALL))
	@JoinColumn(name="usergroup_id")
	var users: UserGroup = UserGroup.ofUsercodes

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "custom_role_id")
	var customRoleDefinition: CustomRoleDefinition = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.BuiltInRoleDefinitionUserType")
	var builtInRoleDefinition: BuiltInRoleDefinition = _

	def roleDefinition = Option(customRoleDefinition) getOrElse builtInRoleDefinition
	def roleDefinition_= (definition: RoleDefinition) = definition match {
		case customDefinition: CustomRoleDefinition => {
			customRoleDefinition = customDefinition
			builtInRoleDefinition = null
		}
		case builtInDefinition: BuiltInRoleDefinition => {
			customRoleDefinition = null
			builtInRoleDefinition = builtInDefinition
		}
		case _ => {
			customRoleDefinition = null
			builtInRoleDefinition = null
		}
	}

	var scope: A
	// this ought not to be necessary, but for some reason the compiler fails to see the type bound on scope and won't
	// assume it's a permissions target
	def scopeAsPermissionsTarget:PermissionsTarget = scope

	def build() = RoleBuilder.build(replaceableRoleDefinition, Option(scope), replaceableRoleDefinition.getName)
	def mayGrant(target: Permission) = Option(replaceableRoleDefinition) map { _.mayGrant(target) } getOrElse (false)
	
	// Provides a route to Department from the scope, so that we can look for custom definitions
	def scopeDepartment: Option[Department]
	
	def replaceableRoleDefinition = scopeDepartment.flatMap { _.replacedRoleDefinitionFor(roleDefinition) }.getOrElse(roleDefinition)

	// If hibernate sets users to null, make a new empty usergroup
	override def postLoad {
		ensureUsers
	}

	def ensureUsers = {
		if (users == null) users = UserGroup.ofUsercodes
		users
	}

}

// TODO DRY this out
object GrantedRole {
	def apply[A <: PermissionsTarget](scope: A, definition: RoleDefinition): GrantedRole[A] =
		(scope match {
			case dept: Department => new DepartmentGrantedRole(dept, definition)
			case module: Module => new ModuleGrantedRole(module, definition)
			case route: Route => new RouteGrantedRole(route, definition)
			case member: Member => new MemberGrantedRole(member, definition)
			case assignment: Assignment => new AssignmentGrantedRole(assignment, definition)
			case group: SmallGroup => new SmallGroupGrantedRole(group, definition)
			case event: SmallGroupEvent => new SmallGroupEventGrantedRole(event, definition)
			case _ => throw new IllegalArgumentException("Cannot define new roles for " + scope)
		}).asInstanceOf[GrantedRole[A]]

	def canDefineFor[A <: PermissionsTarget](scope: A) = scope match {
		case _: Department => true
		case _: Module => true
		case _: Route => true
		case _: Member => true
		case _: Assignment => true
		case _: SmallGroup => true
		case _: SmallGroupEvent => true
		case _ => false
	}
	
	def classObject[A <: PermissionsTarget : ClassTag] = classTag[A] match {
		case t if isSubtype(t, classTag[Department]) => classOf[DepartmentGrantedRole]
		case t if isSubtype(t, classTag[Module]) => classOf[ModuleGrantedRole]
		case t if isSubtype(t, classTag[Route]) => classOf[RouteGrantedRole]
		case t if isSubtype(t, classTag[Member]) => classOf[MemberGrantedRole]
		case t if isSubtype(t, classTag[Assignment]) => classOf[AssignmentGrantedRole]
		case t if isSubtype(t, classTag[SmallGroup]) => classOf[SmallGroupGrantedRole]
		case t if isSubtype(t, classTag[SmallGroupEvent]) => classOf[SmallGroupEventGrantedRole]
		case _ => classOf[GrantedRole[_]]
	}

  private def isSubtype[A,B](self: ClassTag[A], other: ClassTag[B]) = other.runtimeClass.isAssignableFrom(self.runtimeClass)

  def className[A <: PermissionsTarget : ClassTag] = classObject[A].getSimpleName
	def discriminator[A <: PermissionsTarget : ClassTag] = 
		Option(classObject[A].getAnnotation(classOf[DiscriminatorValue])) map { _.value }
}

/* Ok, this is icky, but I can't find any other way. If you need new targets for GrantedRoles, create them below with a new discriminator */
@Entity @DiscriminatorValue("___GLOBAL___") class GloballyGrantedRole extends GrantedRole[PermissionsTarget] {
	def this(definition: RoleDefinition) = {
		this()
		this.roleDefinition = definition
	}

	@transient var scope: PermissionsTarget = null
	
	def scopeDepartment = None
	
	override def build() = RoleBuilder.build(GlobalRoleDefinition(replaceableRoleDefinition), None, replaceableRoleDefinition.getName)
}

/**
 * Wrap a normal RoleDefinition to allow us to make permissions that aren't allowed to be global, global.
 */
case class GlobalRoleDefinition(delegate: RoleDefinition) extends RoleDefinition {
	def permissions(scope: Option[PermissionsTarget]) =
		delegate.permissions(Some(null)).map { 
			case (perm, Some(null)) => (perm, None)
			case (perm, scope) => (perm, scope)
		}

	def subRoles(scope: Option[PermissionsTarget]) = delegate.subRoles(scope)
	def mayGrant(permission: Permission) = delegate.mayGrant(permission)
	def allPermissions(scope: Option[PermissionsTarget]) = delegate.allPermissions(scope)
	def canDelegateThisRolesPermissions = delegate.canDelegateThisRolesPermissions
	def getName = delegate.getName
	def description = delegate.description
	def isAssignable = delegate.isAssignable
}

@Entity @DiscriminatorValue("Department") class DepartmentGrantedRole extends GrantedRole[Department] {
	def this(department: Department, definition: RoleDefinition) = {
		this()
		this.scope = department
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Department = _
	
	def scopeDepartment = Some(scope)
}
@Entity @DiscriminatorValue("Module") class ModuleGrantedRole extends GrantedRole[Module] {
	def this(module: Module, definition: RoleDefinition) = {
		this()
		this.scope = module
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Module = _
	
	def scopeDepartment = Some(scope.department)
}
@Entity @DiscriminatorValue("Route") class RouteGrantedRole extends GrantedRole[Route] {
	def this(route: Route, definition: RoleDefinition) = {
		this()
		this.scope = route
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Route = _
	
	def scopeDepartment = Some(scope.department)
}
@Entity @DiscriminatorValue("Member") class MemberGrantedRole extends GrantedRole[Member] {
	def this(member: Member, definition: RoleDefinition) = {
		this()
		this.scope = member
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Member = _
	
	def scopeDepartment = scope match {
		case student: StudentMember => 
			student.mostSignificantCourseDetails.map { _.latestStudentCourseYearDetails.enrolmentDepartment }.orElse(Option(student.homeDepartment))
		case _ => Option(scope.homeDepartment)
	}
}
@Entity @DiscriminatorValue("Assignment") class AssignmentGrantedRole extends GrantedRole[Assignment] {
	def this(assignment: Assignment, definition: RoleDefinition) = {
		this()
		this.scope = assignment
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: Assignment = _
	
	def scopeDepartment = Some(scope.module.department)
}
@Entity @DiscriminatorValue("SmallGroup") class SmallGroupGrantedRole extends GrantedRole[SmallGroup] {
	def this(group: SmallGroup, definition: RoleDefinition) = {
		this()
		this.scope = group
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: SmallGroup = _
	
	def scopeDepartment = Some(scope.groupSet.module.department)
}
@Entity @DiscriminatorValue("SmallGroupEvent") class SmallGroupEventGrantedRole extends GrantedRole[SmallGroupEvent] {
	def this(event: SmallGroupEvent, definition: RoleDefinition) = {
		this()
		this.scope = event
		this.roleDefinition = definition
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="scope_id")
	@ForeignKey(name="none")
	var scope: SmallGroupEvent = _
	
	def scopeDepartment = Some(scope.group.groupSet.module.department)
}