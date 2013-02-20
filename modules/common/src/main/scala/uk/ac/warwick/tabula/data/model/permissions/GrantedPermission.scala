package uk.ac.warwick.tabula.data.model.permissions
import scala.reflect.BeanProperty

import org.hibernate.annotations.Columns
import org.hibernate.annotations.Type

import javax.persistence._
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity
class GrantedPermission extends GeneratedId with PostLoadBehaviour {
	
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
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.PermissionsTargetUserType")
	@Columns(columns=Array(
			new Column(name="scope_type"),
			new Column(name="scope_id")
	))
	@BeanProperty var scope: PermissionsTarget = _
	
	// If hibernate sets users to null, make a new empty usergroup
	override def postLoad {
		ensureUsers
	}

	def ensureUsers = {
		if (users == null) users = new UserGroup
		users
	}

}