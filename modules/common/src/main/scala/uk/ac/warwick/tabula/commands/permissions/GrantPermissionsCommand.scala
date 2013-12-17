package uk.ac.warwick.tabula.commands.permissions

import scala.collection.JavaConversions._
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.RequestInfo
import scala.reflect.ClassTag

class GrantPermissionsCommand[A <: PermissionsTarget: ClassTag](scope: A) extends Command[GrantedPermission[A]] with SelfValidating {
	
	PermissionCheck(Permissions.RolesAndPermissions.Create, scope)
	
	var permissionsService = Wire[PermissionsService]
	var securityService = Wire[SecurityService]
	
	var permission: Permission = _
	var usercodes: JList[String] = JArrayList()
	var overrideType: Boolean = _
	
	lazy val grantedPermission = permissionsService.getGrantedPermission(scope, permission, overrideType)
	
	def applyInternal() = transactional() {
		val granted = grantedPermission getOrElse GrantedPermission(scope, permission, overrideType)
		
		for (user <- usercodes) granted.users.addUser(user)
		
		permissionsService.saveOrUpdate(granted)
		
		granted
	}
	
	def validate(errors: Errors) {
		if (usercodes.forall { _.isEmptyOrWhitespace }) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else {
			grantedPermission.map { _.users }.foreach { users => 
				val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes") {
					override def alreadyHasCode = usercodes.exists { users.includes(_) }
				}
				
				usercodeValidator.validate(errors)
			}
		}
		
		// Ensure that the current user can do everything that they're trying to grant permissions for
		val user = RequestInfo.fromThread.get.user
		
		if (permission == null) errors.rejectValue("permission", "NotEmpty")
		else if (!user.sysadmin && !securityService.canDelegate(user, permission, scope)) {
			errors.rejectValue("permission", "permissions.cantGiveWhatYouDontHave", Array(permission.description, scope), "")
		}
	}

	def describe(d: Description) = d.properties(
		"scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
		"usercodes" -> usercodes.mkString(","),
		"permission" -> permission,
		"overrideType" -> overrideType)

}