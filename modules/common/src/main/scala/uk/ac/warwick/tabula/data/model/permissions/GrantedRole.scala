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
	var users: UserGroup = new UserGroup

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

	def build() = RoleBuilder.build(roleDefinition, Some(scope), roleDefinition.getName)

	// If hibernate sets users to null, make a new empty usergroup
	override def postLoad {
		ensureUsers
	}

	def ensureUsers = {
		if (users == null) users = new UserGroup
		users
	}

}

object GrantedRole {
	def apply[A <: PermissionsTarget](scope: A, definition: RoleDefinition): GrantedRole[A] =
		(scope match {
			case dept: Department => new DepartmentGrantedRole(dept, definition)
			case module: Module => new ModuleGrantedRole(module, definition)
			case member: Member => new MemberGrantedRole(member, definition)
			case assignment: Assignment => new AssignmentGrantedRole(assignment, definition)
			case _ => throw new IllegalArgumentException("Cannot define new roles for " + scope)
		}).asInstanceOf[GrantedRole[A]]

	def canDefineFor[A <: PermissionsTarget](scope: A) = scope match {
		case _: Department => true
		case _: Module => true
		case _: Member => true
		case _: Assignment => true
		case _ => false
	}
}

/* Ok, this is icky, but I can't find any other way. If you need new targets for GrantedRoles, create them below with a new discriminator */
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
}