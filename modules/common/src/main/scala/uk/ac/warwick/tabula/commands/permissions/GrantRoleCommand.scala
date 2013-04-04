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
import uk.ac.warwick.tabula.RequestInfo
import scala.reflect.ClassTag

class GrantRoleCommand[A <: PermissionsTarget: ClassTag](val scope: A) extends Command[GrantedRole[A]] with SelfValidating {
	
	def this(scope: A, defin: RoleDefinition) = {
		this(scope)
		roleDefinition = defin
	}
	
	PermissionCheck(Permissions.RolesAndPermissions.Create, scope)
	
	var permissionsService = Wire.auto[PermissionsService]
	var securityService = Wire.auto[SecurityService]
	
	var roleDefinition: RoleDefinition = _
	var usercodes: JList[String] = ArrayList()
	
	lazy val grantedRole = permissionsService.getGrantedRole(scope, roleDefinition)
	
	def applyInternal() = transactional() {
		val role = grantedRole getOrElse GrantedRole.init(scope, roleDefinition)
		
		for (user <- usercodes) role.users.addUser(user)
		
		permissionsService.saveOrUpdate(role)
		
		role
	}
	
	def validate(errors: Errors) {
		if (usercodes.find { _.hasText }.isEmpty) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else grantedRole map { _.users } map { users => 
			val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes") {
				override def alreadyHasCode = usercodes.find { users.includes(_) }.isDefined
			}
			
			usercodeValidator.validate(errors)
		}
		
		// Ensure that the current user can do everything that they're trying to grant permissions for
		if (roleDefinition == null) errors.rejectValue("roleDefinition", "NotEmpty")
		else {
			val user = RequestInfo.fromThread.get.user
			if (!user.sysadmin) roleDefinition.allPermissions(Some(scope)) map { permissionAndScope =>
				val (permission, scope) = (permissionAndScope._1, permissionAndScope._2.orNull)
				
				if (!securityService.can(user, permission, scope)) {
					errors.rejectValue("roleDefinition", "permissions.cantGiveWhatYouDontHave", Array(permission, scope), "")
				}
			}
		}
	}

	def describe(d: Description) = d.properties(
		"scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
		"usercodes" -> usercodes.mkString(","),
		"roleDefinition" -> roleDefinition)

}