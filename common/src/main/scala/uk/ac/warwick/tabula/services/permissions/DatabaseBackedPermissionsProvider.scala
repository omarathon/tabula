package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Component
class DatabaseBackedPermissionsProvider extends ScopelessPermissionsProvider {

	var service: PermissionsService = Wire[PermissionsService]

	def getPermissionsFor(user: CurrentUser): Stream[PermissionDefinition] =
		service.getGrantedPermissionsFor[PermissionsTarget](user) map {
			case global: GrantedPermission[_] if global.scopeType == "___GLOBAL___" =>
				PermissionDefinition(global.permission, None, global.overrideType)

			case grantedPermission =>
				PermissionDefinition(grantedPermission.permission, Some(grantedPermission.scope), grantedPermission.overrideType)
		}

	// This isn't exhaustive because we use the cache now - it used to be though.

}