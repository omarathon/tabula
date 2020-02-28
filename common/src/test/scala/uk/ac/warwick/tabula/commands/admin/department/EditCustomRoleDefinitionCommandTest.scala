package uk.ac.warwick.tabula.commands.admin.department

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.permissions.{CustomRoleDefinition, GrantedRole}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.roles.{DepartmentalAdministratorRoleDefinition, ModuleManagerRoleDefinition, UserAccessMgrRoleDefinition}
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{SecurityService, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class EditCustomRoleDefinitionCommandTest extends TestBase with Mockito {

  private trait CommandTestSupport extends EditCustomRoleDefinitionCommandState with PermissionsServiceComponent with SecurityServiceComponent {
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
    val command = new EditCustomRoleDefinitionCommandInternal(department, customRole) with CommandTestSupport
  }

  @Test def init(): Unit = {
    new CommandFixture {
      command.department should be(department)
      command.customRoleDefinition should be(customRole)
      command.name should be("Custom role")
      command.baseDefinition should be(DepartmentalAdministratorRoleDefinition)
    }
  }

  @Test def apply(): Unit = {
    new CommandFixture {
      command.name = "Edited name"
      command.baseDefinition = ModuleManagerRoleDefinition

      command.applyInternal() should be(customRole)
      customRole.name should be("Edited name")
      customRole.baseRoleDefinition should be(ModuleManagerRoleDefinition)
      customRole.department should be(department)

      verify(command.permissionsService, times(1)).saveOrUpdate(customRole)
    }
  }

  @Test def permissions(): Unit = {
    new Fixture {
      val command: EditCustomRoleDefinitionCommandPermissions with EditCustomRoleDefinitionCommandState = new EditCustomRoleDefinitionCommandPermissions with EditCustomRoleDefinitionCommandState {
        override val department: Department = Fixtures.department("in")
        override val customRoleDefinition: CustomRoleDefinition = customRole
      }

      val checking: PermissionsChecking = mock[PermissionsChecking]
      command.permissionsCheck(checking)

      verify(checking, times(1)).PermissionCheck(Permissions.RolesAndPermissions.ManageCustomRoles, customRole)
    }
  }

  @Test(expected = classOf[ItemNotFoundException]) def noDepartment(): Unit = {
    new Fixture {
      val command: EditCustomRoleDefinitionCommandPermissions with EditCustomRoleDefinitionCommandState = new EditCustomRoleDefinitionCommandPermissions with EditCustomRoleDefinitionCommandState {
        override val department: Department = null
        override val customRoleDefinition: CustomRoleDefinition = customRole
      }

      customRole.department = department

      val checking: PermissionsChecking = mock[PermissionsChecking]
      command.permissionsCheck(checking)
    }
  }

  private trait ValidationFixture extends Fixture {
    val d: Department = department

    val command: EditCustomRoleDefinitionCommandValidation with CommandTestSupport = new EditCustomRoleDefinitionCommandValidation with CommandTestSupport {
      val department: Department = d
      val customRoleDefinition: CustomRoleDefinition = customRole
    }
  }

  @Test def validateNoErrors(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.name = "Custom role"
      command.baseDefinition = DepartmentalAdministratorRoleDefinition

      command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns Nil

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(false)
    }
  }

  @Test def validateNoName(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.name = "         "
      command.baseDefinition = DepartmentalAdministratorRoleDefinition

      command.permissionsService.getCustomRoleDefinitionsBasedOn(DepartmentalAdministratorRoleDefinition) returns Nil

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(true)
      errors.getErrorCount should be(1)
      errors.getFieldError.getField should be("name")
      errors.getFieldError.getCodes should contain("customRoleDefinition.name.empty")
    }
  }

  @Test def validateNameTooLong(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.name = (1 to 300).map { _ => "a" }.mkString("")
      command.baseDefinition = DepartmentalAdministratorRoleDefinition

      command.permissionsService.getCustomRoleDefinitionsBasedOn(DepartmentalAdministratorRoleDefinition) returns Nil

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(true)
      errors.getErrorCount should be(1)
      errors.getFieldError.getField should be("name")
      errors.getFieldError.getCodes should contain("customRoleDefinition.name.tooLong")
    }
  }

  @Test def validateNoBaseDefinition(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.name = "Custom role"

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(true)
      errors.getErrorCount should be(1)
      errors.getFieldError.getField should be("baseDefinition")
      errors.getFieldError.getCodes should contain("NotEmpty")
    }
  }

  @Test def validateBaseIsItself(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.name = "Custom role"
      command.baseDefinition = customRole

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(true)
      errors.getErrorCount should be(1)
      errors.getFieldError.getField should be("baseDefinition")
      errors.getFieldError.getCodes should contain("customRoleDefinition.baseIsSelf")
    }
  }

  @Test def validateBaseIsDerivedFromSelf(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.name = "Custom role"

      val derivedCustomRole = new CustomRoleDefinition
      derivedCustomRole.baseRoleDefinition = customRole
      derivedCustomRole.department = department
      department.customRoleDefinitions.add(derivedCustomRole)

      command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns Seq(derivedCustomRole)
      command.permissionsService.getCustomRoleDefinitionsBasedOn(derivedCustomRole) returns Nil

      command.baseDefinition = derivedCustomRole

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(true)
      errors.getErrorCount should be(1)
      errors.getFieldError.getField should be("baseDefinition")
      errors.getFieldError.getCodes should contain("customRoleDefinition.baseIsSelf")
    }
  }

  @Test def validateCantUpliftPermissions(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.name = "Custom role"
      command.baseDefinition = UserAccessMgrRoleDefinition

      command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns Nil

      val otherUser: User = Fixtures.user()

      val grantedBaseRole: GrantedRole[Department] = GrantedRole(department, customRole)
      grantedBaseRole.users.add(otherUser)

      command.permissionsService.getAllGrantedRolesForDefinition(customRole) returns Seq(grantedBaseRole)

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(true)
      // There'll be lots of errors, but don't want to rely on keeping a count of the number of permissions
      // that a UAM has that a dept admin doesn't
      errors.getFieldError.getField should be("baseDefinition")
      errors.getFieldError.getCodes should contain("permissions.cantGiveWhatYouDontHave")
    }
  }

  @Test def description(): Unit = {
    val command: EditCustomRoleDefinitionCommandDescription with EditCustomRoleDefinitionCommandState = new EditCustomRoleDefinitionCommandDescription with EditCustomRoleDefinitionCommandState {
      override val eventName: String = "test"
      val department: Department = Fixtures.department("in")
      val customRoleDefinition = new CustomRoleDefinition
      customRoleDefinition.id = "custom-role"
      customRoleDefinition.department = department
    }

    val d = new DescriptionImpl
    command.describe(d)

    d.allProperties should be(Map(
      "department" -> "in",
      "customRoleDefinition" -> "custom-role"
    ))
  }

}
