package uk.ac.warwick.tabula.services
import uk.ac.warwick.userlookup.GroupService
import org.springframework.beans.factory.annotation.{Autowired,Value}
import uk.ac.warwick.util.core.StringUtils._
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model._
import forms.Extension
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.SubmitPermissionDeniedException
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.permissions.ScopelessPermission
import uk.ac.warwick.tabula.services.permissions.RoleService
import uk.ac.warwick.tabula.roles.Role

/**
 * Checks permissions.
 */
@Service
class SecurityService extends Logging {

	var roleService = Wire.auto[RoleService]

	type Response = Option[Boolean]
	type PermissionChecker = (CurrentUser, Permission, => PermissionsTarget) => Response

	// The possible response types for a permissions check.
	// Continue means continue to the next check, otherwise it will stop and use the returned value.
	// It is an error for all the checks to return Continue, so it makes sense to have one check
	// at the end which never returns Continue.
	val Allow: Response = Some(true)
	val Deny: Response = Some(false)
	val Continue: Response = None // delegate to the next handler

	val checks: Seq[PermissionChecker] = List(checkGod _, checkPermissions _, checkRoles _)

	def checkGod(user: CurrentUser, permission: Permission, scope: => PermissionsTarget): Response = if (user.god) Allow else Continue
	
	private def checkPermissions(allPermissions: Map[Permission, Option[PermissionsTarget]], user: CurrentUser, permission: Permission, scope: => PermissionsTarget): Response = {	
		permission match {
			case permission: ScopelessPermission => if (allPermissions.contains(permission)) Allow else Continue
			case permission => {
				def scopeMatches(permissionScope: => PermissionsTarget, targetScope: => PermissionsTarget): Boolean =					
					// The ID matches, or there exists a parent that matches (recursive)
					permissionScope.id == targetScope.id || targetScope.permissionsParents.exists(scopeMatches(permissionScope, _))
					
				allPermissions.get(permission) match {
					case Some(permissionScope) => permissionScope match {
						case Some(permissionScope) => if (scopeMatches(permissionScope, scope)) Allow else Continue
						case None => Continue
					}
					case None => Continue
				}
			}
		}
	}
	
	def checkPermissions(user: CurrentUser, permission: Permission, scope: => PermissionsTarget): Response = 
			checkPermissions(roleService.getExplicitPermissionsFor(user, scope), user, permission, scope)
			
	def checkRoles(roles: Iterable[Role], user: CurrentUser, permission: Permission, scope: => PermissionsTarget): Response = Some(
		roles exists { role =>
			checkPermissions(role.explicitPermissions.toMap, user, permission, scope) == Allow ||
			checkRoles(role.subRoles, user, permission, scope) == Allow
		}
	)
	
	def checkRoles(user: CurrentUser, permission: Permission, scope: => PermissionsTarget): Response = 
			checkRoles(roleService.getRolesFor(user, scope), user, permission, scope)

	def can(user: CurrentUser, permission: ScopelessPermission) = _can(user, permission, None)
	def can(user: CurrentUser, permission: Permission, scope: => PermissionsTarget) = _can(user, permission, Some(scope)) 
		
	private def _can(user: CurrentUser, permission: Permission, scope: => Option[PermissionsTarget]): Boolean = 
		transactional(readOnly=true) {
			// loop through checks, seeing if any of them return Allow or Deny
			for (check <- checks) {
				val response = check(user, permission, scope orNull)
				response map { canDo =>
					if (debugEnabled) logger.debug("can " + user + " do " + permission + " on " + scope + "? " + (if (canDo) "Yes" else "NO"))
					return canDo
				}
			}
			throw new IllegalStateException("No security rule handled request for " + user + " doing " + permission + " on " + scope)
		}
	
	def check(user: CurrentUser, permission: ScopelessPermission) = _check(user, permission, None)
	def check(user: CurrentUser, permission: Permission, scope: => PermissionsTarget) = _check(user, permission, Some(scope))

	private def _check(user: CurrentUser, permission: Permission, scope: => Option[PermissionsTarget]) = if (!_can(user, permission, scope)) {
		(permission, scope) match {
			case (Permissions.Submission.Create, Some(assignment: Assignment)) => throw new SubmitPermissionDeniedException(assignment)
			case (permission, scope) => throw new PermissionDeniedException(user, permission, scope)
		}
	}
}
