package uk.ac.warwick.tabula.commands.admin.department

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, RoleOverride}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object DeleteCustomRoleOverrideCommand {
  def apply(department: Department, customRoleDefinition: CustomRoleDefinition, roleOverride: RoleOverride) =
    new DeleteCustomRoleOverrideCommandInternal(department, customRoleDefinition, roleOverride)
      with ComposableCommand[RoleOverride]
      with DeleteCustomRoleOverrideCommandDescription
      with DeleteCustomRoleOverrideCommandValidation
      with DeleteCustomRoleOverrideCommandPermissions
      with AutowiringPermissionsServiceComponent
      with AutowiringSecurityServiceComponent
}

trait DeleteCustomRoleOverrideCommandState {
  def department: Department

  def customRoleDefinition: CustomRoleDefinition

  def roleOverride: RoleOverride
}

class DeleteCustomRoleOverrideCommandInternal(val department: Department, val customRoleDefinition: CustomRoleDefinition, val roleOverride: RoleOverride) extends CommandInternal[RoleOverride] with DeleteCustomRoleOverrideCommandState {
  self: PermissionsServiceComponent =>

  override def applyInternal(): RoleOverride = transactional() {
    customRoleDefinition.overrides.remove(roleOverride)

    permissionsService.saveOrUpdate(customRoleDefinition)
    roleOverride
  }
}

trait DeleteCustomRoleOverrideCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: DeleteCustomRoleOverrideCommandState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.mustBeLinked(mandatory(customRoleDefinition), mandatory(department))
    p.mustBeLinked(mandatory(roleOverride), mandatory(customRoleDefinition))
    p.PermissionCheck(Permissions.RolesAndPermissions.ManageCustomRoles, roleOverride)
  }
}

trait DeleteCustomRoleOverrideCommandValidation extends SelfValidating with RoleOverrideDelegationValidation {
  self: DeleteCustomRoleOverrideCommandState
    with PermissionsServiceComponent
    with SecurityServiceComponent =>

  override def validate(errors: Errors): Unit = {
    // Check that we can do the opposite of what we're deleting
    validateCanOverridePermission(customRoleDefinition, roleOverride.permission, !roleOverride.overrideType)(errors, "")
  }
}

trait DeleteCustomRoleOverrideCommandDescription extends Describable[RoleOverride] {
  self: DeleteCustomRoleOverrideCommandState =>
  // describe the thing that's happening.
  override def describe(d: Description): Unit =
    d.customRoleDefinition(customRoleDefinition)
}

