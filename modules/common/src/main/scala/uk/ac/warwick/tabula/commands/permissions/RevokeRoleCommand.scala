package uk.ac.warwick.tabula.commands.permissions

import scala.collection.JavaConversions._
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
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
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import scala.reflect.ClassTag
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.services.SecurityService

class RevokeRoleCommand[A <: PermissionsTarget: ClassTag](val scope: A) extends Command[GrantedRole[A]] with SelfValidating {
	
	def this(scope: A, defin: RoleDefinition) = {
		this(scope)
		roleDefinition = defin
	}

	PermissionCheck(Permissions.RolesAndPermissions.Delete, scope)
	
	var permissionsService = Wire[PermissionsService]
	var securityService = Wire[SecurityService]
	
	var roleDefinition: RoleDefinition = _
	var usercodes: JList[String] = JArrayList()
	
	lazy val grantedRole = permissionsService.getGrantedRole(scope, roleDefinition)
	
	def applyInternal() = transactional() {
		grantedRole map { role =>
			for (user <- usercodes) role.users.removeUser(user)
			
			permissionsService.saveOrUpdate(role)
		}
		
		grantedRole.orNull
	}
	
	def validate(errors: Errors) {
		if (usercodes.forall { _.isEmptyOrWhitespace }) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else {
			grantedRole.map { _.users }.foreach { users => 
				for (code <- usercodes) {
					if (!users.includes(code)) {
						errors.rejectValue("usercodes", "userId.notingroup", Array(code), "")
					}
				}
			}
		}
		
		// Ensure that the current user can delegate everything that they're trying to revoke permissions for
		if (roleDefinition == null) {
			errors.rejectValue("roleDefinition", "NotEmpty")
		} else {
			if (!roleDefinition.isAssignable) errors.rejectValue("roleDefinition", "permissions.roleDefinition.notAssignable")
			val user = RequestInfo.fromThread.get.user

			val permissionsToRevoke = roleDefinition.allPermissions(Some(scope)).keys
			val deniedPermissions = permissionsToRevoke.filterNot(securityService.canDelegate(user,_,scope))
			if ((!deniedPermissions.isEmpty) && (!user.god)) {
				errors.rejectValue("roleDefinition", "permissions.cantRevokeWhatYouDontHave", Array(deniedPermissions.map { _.description }.mkString("\n"), scope),"")
			}
		}
	}

	def describe(d: Description) = d.properties(
		"scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
		"usercodes" -> usercodes.mkString(","),
		"roleDefinition" -> roleDefinition)
	
}