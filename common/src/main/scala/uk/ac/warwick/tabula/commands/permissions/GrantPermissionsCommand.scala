package uk.ac.warwick.tabula.commands.permissions

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringUserLookupComponent, SecurityServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.UsercodeListValidator

import scala.jdk.CollectionConverters._
import scala.reflect._

object GrantPermissionsCommand {
  type Result[A <: PermissionsTarget] = GrantedPermission[A]
  type Command[A <: PermissionsTarget] = Appliable[Result[A]] with PermissionsCommandRequest with PermissionsCommandState[A] with SelfValidating

  def apply[A <: PermissionsTarget : ClassTag](scope: A): Command[A] with PermissionsCommandRequestMutablePermission =
    new GrantPermissionsCommandInternal(scope)
      with ComposableCommand[GrantedPermission[A]]
      with PermissionsCommandRequestMutablePermission
      with GrantPermissionsCommandPermissions
      with GrantPermissionsCommandValidation
      with GrantPermissionsCommandDescription[A]
      with AutowiringPermissionsServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringUserLookupComponent
}

abstract class GrantPermissionsCommandInternal[A <: PermissionsTarget : ClassTag](val scope: A)
  extends CommandInternal[GrantedPermission[A]] with PermissionsCommandState[A] {
  self: PermissionsCommandRequest
    with PermissionsServiceComponent
    with UserLookupComponent =>

  lazy val grantedPermission: Option[GrantedPermission[A]] = permissionsService.getGrantedPermission(scope, permission, overrideType)

  def applyInternal(): GrantedPermission[A] = transactional() {
    val granted = grantedPermission.getOrElse(GrantedPermission(scope, permission, overrideType))

    usercodes.asScala.foreach(granted.users.knownType.addUserId)

    permissionsService.saveOrUpdate(granted)

    // For each usercode that we've added, clear the cache
    usercodes.asScala.foreach { usercode =>
      permissionsService.clearCachesForUser((usercode, classTag[A]))
    }

    granted
  }

}

trait GrantPermissionsCommandValidation extends SelfValidating {
  self: PermissionsCommandRequest
    with PermissionsCommandState[_ <: PermissionsTarget]
    with SecurityServiceComponent
    with UserLookupComponent =>

  def validate(errors: Errors): Unit = {
    if (usercodes.asScala.forall(_.isEmptyOrWhitespace)) {
      errors.rejectValue("usercodes", "NotEmpty")
    } else {
      val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes", staffOnlyForADS = true) {
        override def alreadyHasCode: Boolean = usercodes.asScala.exists { u => grantedPermission.exists(_.users.knownType.includesUserId(u)) }
      }
      usercodeValidator.userLookup = userLookup

      usercodeValidator.validate(errors)
    }

    // Ensure that the current user can do everything that they're trying to grant permissions for
    val user = RequestInfo.fromThread.get.user

    if (permission == null) errors.rejectValue("permission", "NotEmpty")
    else if (!user.sysadmin && !securityService.canDelegate(user, permission, scope)) {
      errors.rejectValue("permission", "permissions.cantGiveWhatYouDontHave", Array(permission.description, scope), "")
    }
  }
}

trait PermissionsCommandState[A <: PermissionsTarget] {
  def scope: A
  def grantedPermission: Option[GrantedPermission[A]]
}

trait PermissionsCommandRequestMutablePermission extends PermissionsCommandRequest {
  var permission: Permission = _
}

trait PermissionsCommandRequest {
  def permission: Permission
  var usercodes: JList[String] = JArrayList()
  var overrideType: Boolean = _
}

trait GrantPermissionsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: PermissionsCommandState[_ <: PermissionsTarget] =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.RolesAndPermissions.Create, mandatory(scope))
  }
}

trait GrantPermissionsCommandDescription[A <: PermissionsTarget] extends Describable[GrantedPermission[A]] {
  self: PermissionsCommandRequest
    with PermissionsCommandState[A] =>

  override lazy val eventName: String = "GrantPermissions"

  def describe(d: Description): Unit = d.properties(
    "scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
    "users" -> usercodes.asScala,
    "permission" -> permission.getName,
    "overrideType" -> overrideType
  )
}


