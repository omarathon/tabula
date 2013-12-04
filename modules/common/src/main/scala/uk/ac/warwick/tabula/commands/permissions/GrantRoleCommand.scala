package uk.ac.warwick.tabula.commands.permissions

import scala.collection.JavaConversions._
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.RequestInfo
import scala.reflect.ClassTag

class GrantRoleCommand[A <: PermissionsTarget : ClassTag](val scope: A) extends Command[GrantedRole[A]] with SelfValidating {

	def this(scope: A, defin: RoleDefinition) = {
		this(scope)
		roleDefinition = defin
	}

	PermissionCheck(Permissions.RolesAndPermissions.Create, scope)

	var permissionsService = Wire[PermissionsService]
	var securityService = Wire[SecurityService]
	var roleDefinition: RoleDefinition = _
	var usercodes: JList[String] = JArrayList()

	lazy val grantedRole = permissionsService.getGrantedRole(scope, roleDefinition)

	def applyInternal() = transactional() {
		val role = grantedRole getOrElse GrantedRole(scope, roleDefinition)

		for (user <- usercodes) role.users.addUser(user)

		permissionsService.saveOrUpdate(role)

		role
	}

	def validate(errors: Errors) {
		if (usercodes.forall { _.isEmptyOrWhitespace }) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else {
			grantedRole.map { _.users }.foreach { users =>
				val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes") {
					override def alreadyHasCode = usercodes.exists { users.includes(_) }
				}
				
				usercodeValidator.validate(errors)
			}
		}

		// Ensure that the current user can delegate everything that they're trying to grant permissions for
		if (roleDefinition == null) {
			errors.rejectValue("roleDefinition", "NotEmpty")
		} else {
			if (!roleDefinition.isAssignable) errors.rejectValue("roleDefinition", "permissions.roleDefinition.notAssignable")
			val user = RequestInfo.fromThread.get.user

			val permissionsToAdd = roleDefinition.allPermissions(Some(scope)).keys
			val deniedPermissions = permissionsToAdd.filterNot(securityService.canDelegate(user,_,scope))
			if ((!deniedPermissions.isEmpty) && (!user.god)) {
				errors.rejectValue("roleDefinition", "permissions.cantGiveWhatYouDontHave", Array(deniedPermissions.mkString("\n"), scope),"")
			}
		}
	}

	def describe(d: Description) = d.properties(
		"scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
		"usercodes" -> usercodes.mkString(","),
		"roleDefinition" -> roleDefinition.getName)

}




