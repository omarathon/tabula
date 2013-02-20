package uk.ac.warwick.tabula.data.model.permissions

import scala.reflect.BeanProperty

import org.hibernate.annotations.Columns
import org.hibernate.annotations.Type

import javax.persistence._
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity
class GrantedPermission extends GeneratedId {
	
	@BeanProperty var userId: String = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.PermissionUserType")
	@BeanProperty var permission: Permission = _
	
	type OverrideType = Boolean
	val Allow: OverrideType = true
	val Deny: OverrideType = false
	
	@BeanProperty var overrideType: OverrideType = _
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.PermissionsTargetUserType")
	@Columns(columns=Array(
			new Column(name="scope_type"),
			new Column(name="scope_id")
	))
	@BeanProperty var scope: PermissionsTarget = _

}