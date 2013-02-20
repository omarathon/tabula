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

@Entity
class GrantedRole extends GeneratedId {
	
	@BeanProperty var userId: String = _
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "custom_base_role_id")
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
	
	def build() = RoleBuilder.build(roleDefinition, Some(scope))

}