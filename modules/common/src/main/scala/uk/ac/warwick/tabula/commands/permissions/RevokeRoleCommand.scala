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

class RevokeRoleCommand[A <: PermissionsTarget: ClassTag](val scope: A) extends Command[GrantedRole[A]] with SelfValidating {
	
	def this(scope: A, defin: RoleDefinition) = {
		this(scope)
		roleDefinition = defin
	}

	PermissionCheck(Permissions.RolesAndPermissions.Delete, scope)
	
	var permissionsService = Wire.auto[PermissionsService]
	
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
		if (usercodes.find { _.hasText }.isEmpty) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else grantedRole map { _.users } map { users => 
			for (code <- usercodes) {
				if (!users.includes(code)) {
					errors.rejectValue("usercodes", "userId.notingroup", Array(code), "")
				}
			}
		}
		
		if (roleDefinition == null) errors.rejectValue("roleDefinition", "NotEmpty")
	}

	def describe(d: Description) = d.properties(
		"scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
		"usercodes" -> usercodes.mkString(","),
		"roleDefinition" -> roleDefinition)
	
}