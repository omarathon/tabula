package uk.ac.warwick.tabula.commands.permissions

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.MitigatingCircumstancesSubmissionFilter
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.roles.{MitigatingCircumstancesOfficerRoleDefinition, RoleDefinition}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringUserLookupComponent, SecurityServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.UserNavigationGeneratorImpl
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._
import scala.reflect._

object RevokeRoleCommand {
  type Result[A <: PermissionsTarget] = Option[GrantedRole[A]]
  type Command[A <: PermissionsTarget] = Appliable[Result[A]] with RoleCommandRequest with RoleCommandState[A] with SelfValidating

  private trait SharedAutowiredCommandDependencies[A <: PermissionsTarget]
    extends RevokeRoleCommandPermissions
      with RevokeRoleCommandValidation
      with RevokeRoleCommandDescription[A]
      with AutowiringPermissionsServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringUserLookupComponent
      with AutowiringMitCircsSubmissionServiceComponent {
    self: RoleCommandState[A] with RoleCommandRequest =>
  }

  def apply[A <: PermissionsTarget : ClassTag](scope: A): Command[A] with RoleCommandRequestMutableRoleDefinition =
    new RevokeRoleCommandInternal(scope)
      with ComposableCommand[Option[GrantedRole[A]]]
      with RoleCommandRequestMutableRoleDefinition
      with SharedAutowiredCommandDependencies[A]

  def apply[A <: PermissionsTarget : ClassTag](scope: A, defin: RoleDefinition): Command[A] =
    new RevokeRoleCommandInternal(scope)
      with ComposableCommand[Option[GrantedRole[A]]]
      with RoleCommandRequest
      with SharedAutowiredCommandDependencies[A] {
      override val roleDefinition: RoleDefinition = defin
    }
}

abstract class RevokeRoleCommandInternal[A <: PermissionsTarget : ClassTag](val scope: A) extends CommandInternal[Option[GrantedRole[A]]] with RoleCommandState[A] {
  self: RoleCommandRequest
    with PermissionsServiceComponent
    with UserLookupComponent =>

  lazy val grantedRole: Option[GrantedRole[A]] = permissionsService.getGrantedRole(scope, roleDefinition)

  def applyInternal(): Option[GrantedRole[A]] = transactional() {
    grantedRole.flatMap { role =>
      usercodes.asScala.foreach(role.users.knownType.removeUserId)

      val result = if (role.users.size == 0) {
        permissionsService.delete(role)
        None
      } else {
        permissionsService.saveOrUpdate(role)
        Some(role)
      }

      // For each usercode that we've removed, clear the cache
      usercodes.asScala.foreach(usercode => {
        permissionsService.clearCachesForUser((usercode, classTag[A]))
        // clear the users navigation cache as well
        UserNavigationGeneratorImpl(usercode, forceUpdate = true)
      })
      result
    }
  }

}

trait RevokeRoleCommandValidation extends SelfValidating {
  self: RoleCommandRequest
    with RoleCommandState[_ <: PermissionsTarget]
    with SecurityServiceComponent
    with MitCircsSubmissionServiceComponent =>

  def validate(errors: Errors): Unit = {
    if (usercodes.asScala.forall(_.isEmptyOrWhitespace)) {
      errors.rejectValue("usercodes", "NotEmpty")
    } else {
      grantedRole.map(_.users).foreach { users =>
        for (code <- usercodes.asScala) {
          if (!users.knownType.includesUserId(code)) {
            errors.rejectValue("usercodes", "userId.notingroup", Array(code), "")
          }
        }
      }
    }

    // Ensure that the current user can delegate everything that they're trying to revoke permissions for
    if (roleDefinition == null) {
      errors.rejectValue("roleDefinition", "NotEmpty")
    } else {
      if (!allowUnassignableRoles && !roleDefinition.isAssignable) errors.rejectValue("roleDefinition", "permissions.roleDefinition.notAssignable")
      val user = RequestInfo.fromThread.get.user

      val permissionsToRevoke = roleDefinition.allPermissions(Some(scope)).keys
      val deniedPermissions = permissionsToRevoke.filterNot { permission =>
        if (allowUnassignableRoles)
          securityService.can(user, permission, scope)
        else
          securityService.canDelegate(user, permission, scope)
      }
      if (deniedPermissions.nonEmpty && !user.god) {
        errors.rejectValue("roleDefinition", "permissions.cantRevokeWhatYouDontHave", Array(deniedPermissions.map(_.description).mkString("\n"), scope), "")
      }

      if (
        // Role is a Mit Circs Officer
        roleDefinition == MitigatingCircumstancesOfficerRoleDefinition &&

        // Scope is a department
        scope.isInstanceOf[Department] &&

        // Removing these usercodes would leave the mit circs officer role definition empty
        grantedRole.toSet
          .flatMap { role: GrantedRole[_ <: PermissionsTarget] => role.users.knownType.members }
          .diff(usercodes.asScala.toSet)
          .isEmpty &&

        // There are existing submissions against this scope
        mitCircsSubmissionService.submissionsForDepartment(scope.asInstanceOf[Department], Nil, MitigatingCircumstancesSubmissionFilter()).nonEmpty
      ) {
        errors.rejectValue("usercodes", "permissions.cantRemoveLastMitCircsOfficer")
      }
    }
  }
}

trait RevokeRoleCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RoleCommandState[_ <: PermissionsTarget] =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.RolesAndPermissions.Delete, mandatory(scope))
  }
}

trait RevokeRoleCommandDescription[A <: PermissionsTarget] extends Describable[Option[GrantedRole[A]]] {
  self: RoleCommandRequest
    with RoleCommandState[A] =>

  override lazy val eventName: String = "RevokeRole"

  def describe(d: Description): Unit = d.properties(
    "scope" -> (scope.getClass.getSimpleName + "[" + scope.id + "]"),
    "users" -> usercodes.asScala,
    "roleDefinition" -> roleDefinition.getName
  )
}
