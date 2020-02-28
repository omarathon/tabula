package uk.ac.warwick.tabula.commands.admin.department

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, RoleOverride}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object EditCustomRoleDefinitionCommand {
  def apply(department: Department, customRoleDefinition: CustomRoleDefinition) =
    new EditCustomRoleDefinitionCommandInternal(department, customRoleDefinition)
      with ComposableCommand[CustomRoleDefinition]
      with EditCustomRoleDefinitionCommandDescription
      with EditCustomRoleDefinitionCommandValidation
      with EditCustomRoleDefinitionCommandPermissions
      with AutowiringPermissionsServiceComponent
      with AutowiringSecurityServiceComponent
}

trait EditCustomRoleDefinitionCommandState extends AddCustomRoleDefinitionCommandState {
  def customRoleDefinition: CustomRoleDefinition
}

class EditCustomRoleDefinitionCommandInternal(val department: Department, val customRoleDefinition: CustomRoleDefinition) extends CommandInternal[CustomRoleDefinition] with EditCustomRoleDefinitionCommandState {
  self: PermissionsServiceComponent =>

  name = customRoleDefinition.name
  baseDefinition = customRoleDefinition.baseRoleDefinition

  override def applyInternal(): CustomRoleDefinition = transactional() {
    customRoleDefinition.name = name
    customRoleDefinition.baseRoleDefinition = baseDefinition

    permissionsService.saveOrUpdate(customRoleDefinition)
    customRoleDefinition
  }
}

trait EditCustomRoleDefinitionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: EditCustomRoleDefinitionCommandState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.mustBeLinked(mandatory(customRoleDefinition), mandatory(department))
    p.PermissionCheck(Permissions.RolesAndPermissions.ManageCustomRoles, customRoleDefinition)
  }
}

trait EditCustomRoleDefinitionCommandValidation extends AddCustomRoleDefinitionCommandValidation with RoleOverrideDelegationValidation {
  self: EditCustomRoleDefinitionCommandState
    with PermissionsServiceComponent
    with SecurityServiceComponent =>

  import RoleOverride._

  override def validate(errors: Errors): Unit = {
    super.validate(errors)

    if (!errors.hasErrors) {
      def isSelfOrDerivedFromSelf(definition: RoleDefinition): Boolean =
        baseDefinition == definition || permissionsService.getCustomRoleDefinitionsBasedOn(definition).exists(isSelfOrDerivedFromSelf)

      if (isSelfOrDerivedFromSelf(customRoleDefinition)) {
        errors.rejectValue("baseDefinition", "customRoleDefinition.baseIsSelf")
      } else if (!baseDefinition.isAssignable) {
        // Make sure the base definition is assignable, because the custom role definition will be
        errors.rejectValue("baseDefinition", "customRoleDefinition.baseDefinition.notAssignable")
      } else {
        // check that you have permissions to effectively "grant" the custom role _after_ the change
        val oldPermissions = customRoleDefinition.baseRoleDefinition.allPermissions(Some(null)).keys.toSet
        val newPermissions = baseDefinition.allPermissions(Some(null)).keys.toSet

        val addedPermissions = newPermissions -- oldPermissions
        val removedPermissions = oldPermissions -- newPermissions

        addedPermissions.foreach { permission =>
          validateCanOverridePermission(customRoleDefinition, permission, Allow)(errors, "baseDefinition")
        }

        removedPermissions.foreach { permission =>
          validateCanOverridePermission(customRoleDefinition, permission, Deny)(errors, "baseDefinition")
        }
      }
    }
  }
}

trait EditCustomRoleDefinitionCommandDescription extends Describable[CustomRoleDefinition] {
  self: EditCustomRoleDefinitionCommandState =>
  // describe the thing that's happening.
  override def describe(d: Description): Unit =
    d.customRoleDefinition(customRoleDefinition)
}
