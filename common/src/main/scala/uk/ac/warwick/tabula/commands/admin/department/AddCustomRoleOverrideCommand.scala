package uk.ac.warwick.tabula.commands.admin.department

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, RoleOverride}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object AddCustomRoleOverrideCommand {
  def apply(department: Department, customRoleDefinition: CustomRoleDefinition) =
    new AddCustomRoleOverrideCommandInternal(department, customRoleDefinition)
      with ComposableCommand[RoleOverride]
      with AddCustomRoleOverrideCommandDescription
      with AddCustomRoleOverrideCommandValidation
      with AddCustomRoleOverrideCommandPermissions
      with AutowiringPermissionsServiceComponent
      with AutowiringSecurityServiceComponent
}

trait AddCustomRoleOverrideCommandState {
  def department: Department

  def customRoleDefinition: CustomRoleDefinition

  var permission: Permission = _
  var overrideType: Boolean = _
}

class AddCustomRoleOverrideCommandInternal(val department: Department, val customRoleDefinition: CustomRoleDefinition) extends CommandInternal[RoleOverride] with AddCustomRoleOverrideCommandState {
  self: PermissionsServiceComponent =>

  override def applyInternal(): RoleOverride = transactional() {
    val o = new RoleOverride
    o.customRoleDefinition = customRoleDefinition
    o.permission = permission
    o.overrideType = overrideType

    customRoleDefinition.overrides.add(o)

    permissionsService.saveOrUpdate(customRoleDefinition)
    o
  }
}

trait AddCustomRoleOverrideCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AddCustomRoleOverrideCommandState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.mustBeLinked(mandatory(customRoleDefinition), mandatory(department))
    p.PermissionCheck(Permissions.RolesAndPermissions.ManageCustomRoles, customRoleDefinition)
  }
}

trait AddCustomRoleOverrideCommandValidation extends SelfValidating with RoleOverrideDelegationValidation {
  self: AddCustomRoleOverrideCommandState
    with PermissionsServiceComponent
    with SecurityServiceComponent =>

  import RoleOverride._

  override def validate(errors: Errors): Unit = {
    // permission must be non empty
    if (permission == null) errors.rejectValue("permission", "NotEmpty")
    else {
      val allRolePermissions = customRoleDefinition.permissions(Some(null))

      // Must not be revoking an existing override
      if (customRoleDefinition.overrides.asScala.exists { o => o.permission == permission }) {
        errors.rejectValue("permission", "customRoleDefinition.override.existingOverride")
      }

      if (overrideType == Allow) {
        // If override is Add, it must not already be a permission
        if (allRolePermissions.exists { case (p, _) => p == permission }) {
          errors.rejectValue("permission", "customRoleDefinition.override.alreadyAllowed")
        }
      } else {
        // Must be an existing permission to revoke
        if (!allRolePermissions.exists { case (p, _) => p == permission }) {
          errors.rejectValue("permission", "customRoleDefinition.override.notAllowed")
        }
      }

      validateCanOverridePermission(customRoleDefinition, permission, overrideType)(errors, "permission")
    }
  }
}

trait RoleOverrideDelegationValidation {
  self: PermissionsServiceComponent
    with SecurityServiceComponent =>

  import RoleOverride._

  def validateCanOverridePermission(customRoleDefinition: CustomRoleDefinition, permission: Permission, overrideType: OverrideType)(errors: Errors, field: String): Unit = {
    // Check that you're not elevating permissions for anyone who already has this custom role that you don't have yourself
    // This recursively checks derived role definitions too
    def checkCanOverridePermission(roleDefinition: CustomRoleDefinition): Unit = {
      val user = RequestInfo.fromThread.get.user

      permissionsService.getAllGrantedRolesForDefinition(roleDefinition)
        .filter(_.ensureUsers.nonEmpty)
        .foreach { grantedRole =>
          val scope: PermissionsTarget = grantedRole.scope.asInstanceOf[PermissionsTarget]

          if (!securityService.canDelegate(user, permission, scope) && !user.god) {
            errors.rejectValue(field, if (overrideType == Allow) "permissions.cantGiveWhatYouDontHave" else "permissions.cantRevokeWhatYouDontHave", Array(permission.description, scope), "")
          }
        }

      permissionsService.getCustomRoleDefinitionsBasedOn(roleDefinition).foreach(checkCanOverridePermission)
    }

    checkCanOverridePermission(customRoleDefinition)
  }
}

trait AddCustomRoleOverrideCommandDescription extends Describable[RoleOverride] {
  self: AddCustomRoleOverrideCommandState =>
  // describe the thing that's happening.
  override def describe(d: Description): Unit =
    d.customRoleDefinition(customRoleDefinition)
}

