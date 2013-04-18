package uk.ac.warwick.tabula.commands.permissions

import scala.collection.JavaConversions._

import org.springframework.validation.Errors

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import scala.reflect.ClassTag

class RevokePermissionsCommand[A <: PermissionsTarget: ClassTag](scope: A) extends Command[GrantedPermission[A]] with SelfValidating {

	PermissionCheck(Permissions.RolesAndPermissions.Delete, scope)
	
	var permissionsService = Wire[PermissionsService]
	
	var permission: Permission = _
	var usercodes: JList[String] = JArrayList()
	var overrideType: Boolean = _
	
	lazy val grantedPermission = permissionsService.getGrantedPermission(scope, permission, overrideType)
	
	def applyInternal() = transactional() {
		grantedPermission map { permission =>
			for (user <- usercodes) permission.users.removeUser(user)
			
			permissionsService.saveOrUpdate(permission)
		}
		
		grantedPermission.orNull
	}
	
	def validate(errors: Errors) {
		if (usercodes.find { _.hasText }.isEmpty) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else grantedPermission map { _.users } map { users => 
			for (code <- usercodes) {
				if (!users.includes(code)) {
					errors.rejectValue("usercodes", "userId.notingroup", Array(code), "")
				}
			}
		}
		
		if (permission == null) errors.rejectValue("permission", "NotEmpty")
	}

	def describe(d: Description) = d.properties(
		"scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
		"usercodes" -> usercodes.mkString(","),
		"permission" -> permission,
		"overrideType" -> overrideType)
	
}