package uk.ac.warwick.tabula.commands.permissions

import scala.collection.JavaConversions._
import scala.beans.BeanProperty
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.helpers.ArrayList
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

class GrantPermissionsCommand[A <: PermissionsTarget : Manifest](scope: A) extends Command[GrantedPermission[A]] with SelfValidating {
	
	PermissionCheck(Permissions.RolesAndPermissions.Create, scope)
	
	var permissionsService = Wire.auto[PermissionsService]
	var securityService = Wire.auto[SecurityService]
	
	@BeanProperty var permission: Permission = _
	@BeanProperty var usercodes: JList[String] = ArrayList()
	@BeanProperty var overrideType: Boolean = _
	
	lazy val grantedPermission = permissionsService.getGrantedPermission(scope, permission, overrideType)
	
	def applyInternal() = transactional() {
		val granted = grantedPermission getOrElse GrantedPermission.init(scope, permission, overrideType)
		
		for (user <- usercodes) granted.users.addUser(user)
		
		permissionsService.saveOrUpdate(granted)
		
		granted
	}
	
	def validate(errors: Errors) {
		if (usercodes.find { _.hasText }.isEmpty) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else grantedPermission map { _.users } map { users => 
			val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes") {
				override def alreadyHasCode = usercodes.find { users.includes(_) }.isDefined
			}
			
			usercodeValidator.validate(errors)
		}
		
		// Ensure that the current user can do everything that they're trying to grant permissions for
		val user = RequestInfo.fromThread.get.user
		
		if (permission == null) errors.rejectValue("permission", "NotEmpty")
		else if (!user.sysadmin && !securityService.can(user, permission, scope)) {
			errors.rejectValue("permission", "permissions.cantGiveWhatYouDontHave", Array(permission, scope), "")
		}
	}

	def describe(d: Description) = d.properties(
		"scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
		"usercodes" -> usercodes.mkString(","),
		"permission" -> permission,
		"overrideType" -> overrideType)

}