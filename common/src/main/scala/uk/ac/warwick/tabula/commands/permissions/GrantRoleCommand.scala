package uk.ac.warwick.tabula.commands.permissions

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringUserLookupComponent, SecurityServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.UserNavigationGeneratorImpl
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.UsercodeListValidator

import scala.jdk.CollectionConverters._
import scala.reflect._

object GrantRoleCommand {
  type Result[A <: PermissionsTarget] = GrantedRole[A]
  type Command[A <: PermissionsTarget] = Appliable[Result[A]] with RoleCommandRequest with RoleCommandState[A] with SelfValidating

  def apply[A <: PermissionsTarget : ClassTag](scope: A): Command[A] with RoleCommandRequestMutableRoleDefinition =
    new GrantRoleCommandInternal(scope)
      with ComposableCommand[GrantedRole[A]]
      with RoleCommandRequestMutableRoleDefinition
      with GrantRoleCommandPermissions
      with GrantRoleCommandValidation
      with GrantRoleCommandDescription[A]
      with AutowiringPermissionsServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringUserLookupComponent

  def apply[A <: PermissionsTarget : ClassTag](scope: A, defin: RoleDefinition): Command[A] =
    new GrantRoleCommandInternal(scope)
      with ComposableCommand[GrantedRole[A]]
      with RoleCommandRequest
      with GrantRoleCommandPermissions
      with GrantRoleCommandValidation
      with GrantRoleCommandDescription[A]
      with AutowiringPermissionsServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringUserLookupComponent {
      override val roleDefinition: RoleDefinition = defin
    }
}

abstract class GrantRoleCommandInternal[A <: PermissionsTarget : ClassTag](val scope: A) extends CommandInternal[GrantedRole[A]] with RoleCommandState[A] {
  self: RoleCommandRequest
    with PermissionsServiceComponent
    with UserLookupComponent =>

  lazy val grantedRole: Option[GrantedRole[A]] = permissionsService.getGrantedRole(scope, roleDefinition)

  def applyInternal(): GrantedRole[A] = transactional() {
    val role = grantedRole.getOrElse(GrantedRole(scope, roleDefinition))

    usercodes.asScala.foreach(role.users.knownType.addUserId)

    permissionsService.saveOrUpdate(role)

    // For each usercode that we've added, clear the cache
    usercodes.asScala.foreach { usercode =>
      permissionsService.clearCachesForUser((usercode, classTag[A]))
      // clear the users navigation cache as well
      UserNavigationGeneratorImpl(usercode, forceUpdate = true)
    }

    role
  }

}

trait GrantRoleCommandValidation extends SelfValidating {
  self: RoleCommandRequest
    with RoleCommandState[_ <: PermissionsTarget]
    with SecurityServiceComponent
    with UserLookupComponent =>

  def validate(errors: Errors): Unit = {
    if (usercodes.asScala.forall(_.isEmptyOrWhitespace)) {
      errors.rejectValue("usercodes", "NotEmpty")
    } else {
      val usercodeValidator = new UsercodeListValidator(usercodes, "usercodes", staffOnlyForADS = true) {
        override def alreadyHasCode: Boolean = usercodes.asScala.exists { u => grantedRole.exists(_.users.knownType.includesUserId(u)) }
      }
      usercodeValidator.userLookup = userLookup

      usercodeValidator.validate(errors)
    }

    // Ensure that the current user can delegate everything that they're trying to grant permissions for
    if (roleDefinition == null) {
      errors.rejectValue("roleDefinition", "NotEmpty")
    } else {
      if (!allowUnassignableRoles && !roleDefinition.isAssignable) errors.rejectValue("roleDefinition", "permissions.roleDefinition.notAssignable")
      val user = RequestInfo.fromThread.get.user

      val permissionsToAdd = roleDefinition.allPermissions(Some(scope)).keys
      val deniedPermissions = permissionsToAdd.filterNot { permission =>
        if (allowUnassignableRoles)
          securityService.can(user, permission, scope)
        else
          securityService.canDelegate(user, permission, scope)
      }
      if (deniedPermissions.nonEmpty && !user.god) {
        errors.rejectValue("roleDefinition", "permissions.cantGiveWhatYouDontHave", Array(deniedPermissions.map(_.description).mkString("\n"), scope), "")
      }
    }
  }
}

trait RoleCommandState[A <: PermissionsTarget] {
  def scope: A
  def grantedRole: Option[GrantedRole[A]]
  val allowUnassignableRoles: Boolean = false
}

trait RoleCommandRequestMutableRoleDefinition extends RoleCommandRequest {
  var roleDefinition: RoleDefinition = _
}

trait RoleCommandRequest {
  def roleDefinition: RoleDefinition
  var usercodes: JList[String] = JArrayList()
}

trait GrantRoleCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RoleCommandState[_ <: PermissionsTarget] =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.RolesAndPermissions.Create, mandatory(scope))
  }
}

trait GrantRoleCommandDescription[A <: PermissionsTarget] extends Describable[GrantedRole[A]] {
  self: RoleCommandRequest
    with RoleCommandState[A] =>

  override lazy val eventName: String = "GrantRole"

  def describe(d: Description): Unit = d.properties(
    "scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
    "users" -> usercodes.asScala,
    "roleDefinition" -> roleDefinition.getName
  )
}



