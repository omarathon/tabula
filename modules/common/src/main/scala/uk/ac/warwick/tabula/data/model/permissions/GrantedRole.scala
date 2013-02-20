package uk.ac.warwick.tabula.data.model.permissions
import scala.reflect.BeanProperty
import org.hibernate.annotations.Columns
import org.hibernate.annotations.Type
import javax.persistence._
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.roles.RoleBuilder
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.PostLoadBehaviour

@Entity
class GrantedRole extends GeneratedId with PostLoadBehaviour {
	
	@OneToOne(cascade=Array(CascadeType.ALL))
	@JoinColumn(name="usergroup_id")
	@BeanProperty var users: UserGroup = new UserGroup
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "custom_role_id")
	@BeanProperty var customRoleDefinition: CustomRoleDefinition = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.BuiltInRoleDefinitionUserType")
	@BeanProperty var builtInRoleDefinition: BuiltInRoleDefinition = _
	
	def roleDefinition = Option(customRoleDefinition) getOrElse builtInRoleDefinition
	def roleDefinition_(definition: RoleDefinition) = definition match {
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
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.PermissionsTargetUserType")
	@Columns(columns=Array(
			new Column(name="scope_type"),
			new Column(name="scope_id")
	))
	@BeanProperty var scope: PermissionsTarget = _
	
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