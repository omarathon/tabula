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
import uk.ac.warwick.tabula.permissions.{ScopelessPermission, SelectorPermission, Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.validators.UsercodeListValidator
import uk.ac.warwick.tabula.RequestInfo
import scala.reflect.ClassTag
import uk.ac.warwick.tabula.commands.permissions.GrantRoleCommand.getDeniedPermissions
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

class GrantRoleCommand[A <: PermissionsTarget : ClassTag](val scope: A) extends Command[GrantedRole[A]] with SelfValidating {

	def this(scope: A, defin: RoleDefinition) = {
		this(scope)
		roleDefinition = defin
	}

	PermissionCheck(Permissions.RolesAndPermissions.Create, scope)

	var permissionsService = Wire.auto[PermissionsService]

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
		if (usercodes.find {
			_.hasText
		}.isEmpty) {
			errors.rejectValue("usercodes", "NotEmpty")
		} else grantedRole map {
			_.users
		} map {
			users =>
				val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes") {
					override def alreadyHasCode = usercodes.find {
						users.includes(_)
					}.isDefined
				}

				usercodeValidator.validate(errors)
		}


		// Ensure that the current user can do everything that they're trying to grant permissions for
		if (roleDefinition == null) errors.rejectValue("roleDefinition", "NotEmpty")
		else {
			if (!roleDefinition.isAssignable) errors.rejectValue("roleDefinition", "permissions.roleDefinition.notAssignable")
			val user = RequestInfo.fromThread.get.user
			val userRoles = permissionsService.getAllGrantedRolesFor(user)

			val delegatablePermsMaps = userRoles.map(r => r.roleDefinition.delegatablePermissions(Some(r.scopeAsPermissionsTarget)))
			// convert a Seq[Map[Permission, Option[Target]] into a Map[Permission, Seq[Option[Target]]
			val allDelegatablePerms = delegatablePermsMaps.map(_.toList).flatten
			val delegatablePerms = allDelegatablePerms.groupBy(_._1).map {
				case (k, v) => k -> v.map(_._2).distinct
			}
			//TODO could optimise this further by removing going through the Seq[Option[Target]] lists and removing any which
			// are permissions children of another item in the list. But that probably doesn't happen often


			val deniedPermissions = getDeniedPermissions(delegatablePerms, roleDefinition, scope)
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

object GrantRoleCommand extends Logging {

	// Check that each permission contained in the RoleDefinition we want to add,
	// is permitted, for the intended scope, by the list of permissions/scopes pairs we can delegate
	def getDeniedPermissions(delegatablePerms: Map[Permission, Seq[Option[PermissionsTarget]]], roleDefinition: RoleDefinition, scope: PermissionsTarget) = {

		val permissionsToAdd = roleDefinition.allPermissions(Some(scope))
		val scopeWithParents = scope +: scope.permissionsParents.toSeq.distinct

		permissionsToAdd.flatMap {
			// for a scopeless perm, we don't need to check the scopes
			case (perm: ScopelessPermission, _) => {
				if (delegatablePerms.contains(perm)) {
					None
				} else {
					Some(perm)
				}
			}
			// it's a scoped perm. Need to check that the scope matches
			case (perm: Permission, _) =>
				if (delegatablePerms.contains(perm)) {
					val delegatableScopes = delegatablePerms(perm)
					// It's a match if there's an exact match for the scope...
					if  (delegatableScopes.contains(scope)
						// ...or if one of the delegatable scopes is "None",
						// then we have this permission globally in the delegatable role, so we can
						// grant it for any scope...
						|| (delegatableScopes.contains(None))
						//  ... or if any of the delegatable scopes match any of the intended scopes' parents
						// flatten should be a no-op here as we've already verified there are no Nones above.
						|| delegatableScopes.flatten.exists(s=>scopeWithParents.contains(s))) {
						None
					} else {
						// no match for the scope- add this permission to the list of rejects.
						Some(perm)
					}
				} else {
					// no match for the permission - add this permission to the list of rejects
					Some(perm)
				}
		}
	}
}



