package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, GrantedRole, RoleOverride}
import uk.ac.warwick.tabula.services.{SecurityService, SecurityServiceComponent}
import uk.ac.warwick.userlookup.User

class AddCustomRoleOverrideCommandTest extends TestBase with Mockito {

  private trait CommandTestSupport extends AddCustomRoleOverrideCommandState with PermissionsServiceComponent with SecurityServiceComponent {
    val permissionsService: PermissionsService = smartMock[PermissionsService]
    val securityService: SecurityService = smartMock[SecurityService]
  }

  private trait Fixture {
    val department: Department = Fixtures.department("in")

    val customRole = new CustomRoleDefinition
    customRole.id = "custom"
    customRole.name = "Custom role"
    customRole.baseRoleDefinition = DepartmentalAdministratorRoleDefinition
    customRole.department = department
    department.customRoleDefinitions.add(customRole)
  }

  private trait CommandFixture extends Fixture {
    val command = new AddCustomRoleOverrideCommandInternal(department, customRole) with CommandTestSupport
  }

  @Test def init(): Unit = {
    new CommandFixture {
      command.department should be(department)
      command.customRoleDefinition should be(customRole)
    }
  }

  @Test def apply(): Unit = {
    new CommandFixture {
      command.permission = Permissions.Module.ManageAssignments
      command.overrideType = RoleOverride.Allow

      val created: RoleOverride = command.applyInternal()
      created.permission should be(Permissions.Module.ManageAssignments)
      created.overrideType should be(RoleOverride.Allow)
      created.customRoleDefinition should be(customRole)

      verify(command.permissionsService, times(1)).saveOrUpdate(customRole)
    }
  }

  @Test def permissions(): Unit = {
    new Fixture {
      val d: Department = department

      val command: AddCustomRoleOverrideCommandPermissions with AddCustomRoleOverrideCommandState = new AddCustomRoleOverrideCommandPermissions with AddCustomRoleOverrideCommandState {
        override val department: Department = d
        override val customRoleDefinition: CustomRoleDefinition = customRole
      }

      val checking: PermissionsChecking = mock[PermissionsChecking]
      command.permissionsCheck(checking)

      verify(checking, times(1)).PermissionCheck(Permissions.RolesAndPermissions.ManageCustomRoles, customRole)
    }
  }

  @Test(expected = classOf[ItemNotFoundException]) def noDepartment(): Unit = {
    new Fixture {
      val command: AddCustomRoleOverrideCommandPermissions with AddCustomRoleOverrideCommandState = new AddCustomRoleOverrideCommandPermissions with AddCustomRoleOverrideCommandState {
        override val department: Department = null
        override val customRoleDefinition: CustomRoleDefinition = customRole
      }

      val checking: PermissionsChecking = mock[PermissionsChecking]
      command.permissionsCheck(checking)
    }
  }

  private trait ValidationFixture extends Fixture {
    val d: Department = department

    val command: AddCustomRoleOverrideCommandValidation with CommandTestSupport = new AddCustomRoleOverrideCommandValidation with CommandTestSupport {
      val department: Department = d
      val customRoleDefinition: CustomRoleDefinition = customRole
    }
  }

  @Test def validateNoErrors(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.permission = Permissions.Module.ManageAssignments
      command.overrideType = RoleOverride.Deny

      command.permissionsService.getAllGrantedRolesForDefinition(customRole) returns Nil
      command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns Nil

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(false)
    }
  }

  @Test def validateNoPermission(): Unit = {
    new ValidationFixture {
      command.overrideType = RoleOverride.Allow

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(true)
      errors.getErrorCount should be(1)
      errors.getFieldError.getField should be("permission")
      errors.getFieldError.getCodes should contain("NotEmpty")
    }
  }

  @Test def validateExistingOverride(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.permission = Permissions.Module.ManageAssignments
      command.overrideType = RoleOverride.Deny

      command.permissionsService.getAllGrantedRolesForDefinition(customRole) returns Nil
      command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns Nil

      val existing = new RoleOverride
      existing.permission = Permissions.Module.ManageAssignments
      existing.overrideType = RoleOverride.Allow

      customRole.overrides.add(existing)

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(true)
      errors.getErrorCount should be(1)
      errors.getFieldError.getField should be("permission")
      errors.getFieldError.getCodes should contain("customRoleDefinition.override.existingOverride")
    }
  }

  @Test def validateAlreadyAllowed(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.permission = Permissions.Module.ManageAssignments
      command.overrideType = RoleOverride.Allow

      command.permissionsService.getAllGrantedRolesForDefinition(customRole) returns Nil
      command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns Nil

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(true)
      errors.getErrorCount should be(1)
      errors.getFieldError.getField should be("permission")
      errors.getFieldError.getCodes should contain("customRoleDefinition.override.alreadyAllowed")
    }
  }

  @Test def validateAlreadyNotAllowed(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.permission = Permissions.ImportSystemData
      command.overrideType = RoleOverride.Deny

      command.permissionsService.getAllGrantedRolesForDefinition(customRole) returns Nil
      command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns Nil

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(true)
      errors.getErrorCount should be(1)
      errors.getFieldError.getField should be("permission")
      errors.getFieldError.getCodes should contain("customRoleDefinition.override.notAllowed")
    }
  }

  @Test def validateCantGiveWhatYouDontHave(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.permission = Permissions.ImportSystemData
      command.overrideType = RoleOverride.Allow

      val otherUser: User = Fixtures.user()

      val grantedBaseRole: GrantedRole[Department] = GrantedRole(department, customRole)
      grantedBaseRole.users.add(otherUser)

      command.permissionsService.getAllGrantedRolesForDefinition(customRole) returns Seq(grantedBaseRole)

      val derivedCustomRole = new CustomRoleDefinition
      derivedCustomRole.baseRoleDefinition = customRole
      derivedCustomRole.department = department
      department.customRoleDefinitions.add(derivedCustomRole)

      command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns Seq(derivedCustomRole)

      val grantedDerivedRole: GrantedRole[Department] = GrantedRole(department, derivedCustomRole)
      grantedDerivedRole.users.add(otherUser)

      command.permissionsService.getAllGrantedRolesForDefinition(derivedCustomRole) returns Seq(grantedDerivedRole)
      command.permissionsService.getCustomRoleDefinitionsBasedOn(derivedCustomRole) returns Nil

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(true)
      errors.getErrorCount should be(2)
      errors.getFieldError.getField should be("permission")
      errors.getFieldError.getCodes should contain("permissions.cantGiveWhatYouDontHave")
    }
  }

  @Test def description(): Unit = {
    new Fixture {
      val dept: Department = department

      val command: AddCustomRoleOverrideCommandDescription with AddCustomRoleOverrideCommandState = new AddCustomRoleOverrideCommandDescription with AddCustomRoleOverrideCommandState {
        override val eventName: String = "test"
        val department: Department = dept
        val customRoleDefinition: CustomRoleDefinition = customRole
      }

      val d = new DescriptionImpl
      command.describe(d)

      d.allProperties should be(Map(
        "department" -> "in",
        "customRoleDefinition" -> "custom"
      ))
    }
  }

}
