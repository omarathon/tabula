package uk.ac.warwick.tabula.data.model.permissions

import org.hibernate.annotations.Type
import javax.persistence._
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.HibernateVersioned

@Entity
class RoleOverride extends GeneratedId with HibernateVersioned with PermissionsTarget {
	
	// optional link to some CustomRoleDefinition
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "custom_role_definition_id")
	var customRoleDefinition: CustomRoleDefinition = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.PermissionUserType")
	var permission: Permission = _
	
	type OverrideType = Boolean
	@Transient val Allow: OverrideType = true
	@Transient val Deny: OverrideType = false
	
	var overrideType: OverrideType = _
	
	def permissionsParents = Seq(Option(customRoleDefinition)).flatten

}