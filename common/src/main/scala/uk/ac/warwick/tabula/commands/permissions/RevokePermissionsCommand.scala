package uk.ac.warwick.tabula.commands.permissions

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringUserLookupComponent, SecurityServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.reflect._

object RevokePermissionsCommand {
  type Result[A <: PermissionsTarget] = GrantedPermission[A]
  type Command[A <: PermissionsTarget] = Appliable[Result[A]] with PermissionsCommandRequest with PermissionsCommandState[A] with SelfValidating

  def apply[A <: PermissionsTarget : ClassTag](scope: A): Command[A] with PermissionsCommandRequestMutablePermission =
    new RevokePermissionsCommandInternal(scope)
      with ComposableCommand[GrantedPermission[A]]
      with PermissionsCommandRequestMutablePermission
      with RevokePermissionsCommandPermissions
      with RevokePermissionsCommandValidation
      with RevokePermissionsCommandDescription[A]
      with AutowiringPermissionsServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringUserLookupComponent
}

abstract class RevokePermissionsCommandInternal[A <: PermissionsTarget : ClassTag](val scope: A)
  extends CommandInternal[GrantedPermission[A]] with PermissionsCommandState[A] {
  self: PermissionsCommandRequest
    with PermissionsServiceComponent
    with UserLookupComponent =>

  lazy val grantedPermission: Option[GrantedPermission[A]] = permissionsService.getGrantedPermission(scope, permission, overrideType)

  def applyInternal(): GrantedPermission[A] = transactional() {
    grantedPermission.foreach { permission =>
      usercodes.asScala.foreach(permission.users.knownType.removeUserId)

      permissionsService.saveOrUpdate(permission)

      // For each usercode that we've removed, clear the cache
      usercodes.asScala.foreach { usercode =>
        permissionsService.clearCachesForUser((usercode, classTag[A]))
      }
    }

    grantedPermission.orNull
  }

}

trait RevokePermissionsCommandValidation extends SelfValidating {
  self: PermissionsCommandRequest
    with PermissionsCommandState[_ <: PermissionsTarget]
    with SecurityServiceComponent =>

  def validate(errors: Errors) {
    if (usercodes.asScala.forall(_.isEmptyOrWhitespace)) {
      errors.rejectValue("usercodes", "NotEmpty")
    } else {
      grantedPermission.map(_.users).foreach { users =>
        for (code <- usercodes.asScala) {
          if (!users.knownType.includesUserId(code)) {
            errors.rejectValue("usercodes", "userId.notingroup", Array(code), "")
          }
        }
      }
    }

    // Ensure that the current user can do everything that they're trying to grant permissions for
    val user = RequestInfo.fromThread.get.user

    if (permission == null) errors.rejectValue("permission", "NotEmpty")
    else if (!user.sysadmin && !securityService.canDelegate(user, permission, scope)) {
      errors.rejectValue("permission", "permissions.cantRevokeWhatYouDontHave", Array(permission.description, scope), "")
    }
  }
}

trait RevokePermissionsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: PermissionsCommandState[_ <: PermissionsTarget] =>

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.RolesAndPermissions.Delete, mandatory(scope))
  }
}

trait RevokePermissionsCommandDescription[A <: PermissionsTarget] extends Describable[GrantedPermission[A]] {
  self: PermissionsCommandRequest
    with PermissionsCommandState[A] =>

  override lazy val eventName: String = "RevokePermissions"

  def describe(d: Description): Unit = d.properties(
    "scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
    "users" -> usercodes.asScala,
    "permission" -> permission.getName,
    "overrideType" -> overrideType
  )
}


