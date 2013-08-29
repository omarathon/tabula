package uk.ac.warwick.tabula.services
import uk.ac.warwick.userlookup.GroupService
import org.springframework.beans.factory.annotation.{Autowired,Value}
import uk.ac.warwick.tabula.helpers.StringUtils._
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
import scala.annotation.tailrec
import uk.ac.warwick.tabula.helpers.RequestLevelCaching
import uk.ac.warwick.tabula.permissions.SelectorPermission

/**
 * Checks permissions.
 */
@Service
class SecurityService extends Logging with RequestLevelCaching[(CurrentUser, Permission, PermissionsTarget), Option[Boolean]] {

	var roleService = Wire.auto[RoleService]

	type Response = Option[Boolean]
	type PermissionChecker = (CurrentUser, Permission, PermissionsTarget) => Response

	// The possible response types for a permissions check.
	// Continue means continue to the next check, otherwise it will stop and use the returned value.
	// It is an error for all the checks to return Continue, so it makes sense to have one check
	// at the end which never returns Continue.
	val Allow: Response = Some(true)
	val Deny: Response = Some(false)
	val Continue: Response = None // delegate to the next handler

	val checks: Seq[PermissionChecker] = List(checkRuntimeMember _, checkGod _, checkPermissions _, checkRoles _)
	
	def checkRuntimeMember(user: CurrentUser, permission: Permission, scope: PermissionsTarget): Response = scope match {
		case ignore: RuntimeMember => Deny
		case _ => Continue
	}

	def checkGod(user: CurrentUser, permission: Permission, scope: PermissionsTarget): Response = if (user.god) Allow else Continue
	
	private def checkScopedPermission(
		allPermissions: Map[Permission, Option[PermissionsTarget]], 
		user: CurrentUser, 
		permission: Permission, 
		scope: PermissionsTarget
	): Response = {
		def scopeMatches(permissionScope: PermissionsTarget, targetScope: PermissionsTarget): Boolean =
			// The ID matches, or there exists a parent that matches (recursive)
			permissionScope == targetScope || targetScope.permissionsParents.exists(scopeMatches(permissionScope, _))
			
		val matchingPermission = permission match {
			case selectorPerm: SelectorPermission[_] => allPermissions.find {
				case (otherSelectorPerm: SelectorPermission[_], target)
					if (otherSelectorPerm.getClass == selectorPerm.getClass) && 
						 (selectorPerm <= otherSelectorPerm) => true
				case _ => false
			} map { case (_, target) => target }
			case _ => allPermissions.get(permission) 
		}
		
		matchingPermission match {
			case Some(permissionScope) => permissionScope match {
				case Some(permissionScope) => if (scopeMatches(permissionScope, scope)) Allow else Continue
				case None => 
					if (scope != null) Allow // Global permission
					else Continue
			}
			case None => Continue
		}
	}
	
	def checkPermissions(
		allPermissions: Map[Permission, Option[PermissionsTarget]], 
		user: CurrentUser, 
		permission: Permission, 
		scope: PermissionsTarget
	): Response =
		if (allPermissions == null || allPermissions.isEmpty) Continue
		else permission match {
			case _: ScopelessPermission => if (allPermissions.contains(permission)) Allow else Continue
			case _ => checkScopedPermission(allPermissions, user, permission, scope)
		}
	
	def checkPermissions(user: CurrentUser, permission: Permission, scope: PermissionsTarget): Response = {
		val explicitPermissions = roleService.getExplicitPermissionsFor(user, scope)
		if (explicitPermissions == null || explicitPermissions.isEmpty) Continue
		else {
			val (allow, deny) = explicitPermissions.partition(_.permissionType)
			
			// Confusingly, we check for an "Allow" for the deny perms and then immediately deny it
			val denyPerms = (deny map { defn => (defn.permission -> defn.scope) }).toMap
			
			if (checkPermissions(denyPerms, user, permission, scope) != Continue) Deny
			else {
				val allowPerms = (allow map { defn => (defn.permission -> defn.scope) }).toMap
			
				checkPermissions(allowPerms, user, permission, scope)
			}
		}
	}
			
	// By using Some() here, we ensure that we return Deny if there isn't a role match - this must be the LAST permissions provider
	def checkRoles(roles: Iterable[Role], user: CurrentUser, permission: Permission, scope: PermissionsTarget): Response = Some(
		roles != null && (roles exists { role =>			
			checkPermissions(role.explicitPermissions, user, permission, scope) == Allow ||
			checkRoles(role.subRoles, user, permission, scope) == Allow
		})
	)
	
	def checkRoles(user: CurrentUser, permission: Permission, scope: PermissionsTarget): Response =
		checkRoles(roleService.getRolesFor(user, scope), user, permission, scope)

	def can(user: CurrentUser, permission: ScopelessPermission) = _can(user, permission, None)
	def can(user: CurrentUser, permission: Permission, scope: PermissionsTarget) = _can(user, permission, Option(scope)) 
		
	private def _can(user: CurrentUser, permission: Permission, scope: Option[PermissionsTarget]): Boolean = transactional(readOnly=true) {
		// Lazily go through the checks using a view, and try to get the first one that's Allow or Deny
		val result: Response = cachedBy((user, permission, scope.orNull)) {
			checks.view.flatMap { _(user, permission, scope.orNull ) }.headOption
		}

		result.map { canDo =>
			if (debugEnabled) logger.debug("can " + user + " do " + permission + " on " + scope + "? " + (if (canDo) "Yes" else "NO"))
			canDo
		} getOrElse {
			throw new IllegalStateException("No security rule handled request for " + user + " doing " + permission + " on " + scope)
		}
	}
	
	def check(user: CurrentUser, permission: ScopelessPermission) = _check(user, permission, None)
	def check(user: CurrentUser, permission: Permission, scope: PermissionsTarget) = _check(user, permission, Option(scope))

	private def _check(user: CurrentUser, permission: Permission, scope: Option[PermissionsTarget]) = if (!_can(user, permission, scope)) {
		(permission, scope) match {
			case (Permissions.Submission.Create, Some(assignment: Assignment)) => throw new SubmitPermissionDeniedException(assignment)
			case (permission, scope) => throw new PermissionDeniedException(user, permission, scope)
		}
	}
}
