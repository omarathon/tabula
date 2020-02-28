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

import scala.jdk.CollectionConverters._

class DeleteCustomRoleOverrideCommandTest extends TestBase with Mockito {

  private trait CommandTestSupport extends DeleteCustomRoleOverrideCommandState with PermissionsServiceComponent with SecurityServiceComponent {
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

    val roleOverride = new RoleOverride
    roleOverride.permission = Permissions.Module.ManageAssignments
    roleOverride.overrideType = RoleOverride.Allow
    roleOverride.customRoleDefinition = customRole
    customRole.overrides.add(roleOverride)
  }

  private trait CommandFixture extends Fixture {
    val command = new DeleteCustomRoleOverrideCommandInternal(department, customRole, roleOverride) with CommandTestSupport
  }

  @Test def init(): Unit = {
    new CommandFixture {
      command.department should be(department)
      command.customRoleDefinition should be(customRole)
      command.roleOverride should be(roleOverride)
    }
  }

  @Test def apply(): Unit = {
    new CommandFixture {
      command.applyInternal() should be(roleOverride)
      customRole.overrides.asScala should be(Symbol("empty"))

      verify(command.permissionsService, times(1)).saveOrUpdate(customRole)
    }
  }

  @Test def permissions(): Unit = {
    new Fixture {
      val d: Department = department
      val o: RoleOverride = roleOverride

      val command: DeleteCustomRoleOverrideCommandPermissions with DeleteCustomRoleOverrideCommandState = new DeleteCustomRoleOverrideCommandPermissions with DeleteCustomRoleOverrideCommandState {
        override val department: Department = d
        override val customRoleDefinition: CustomRoleDefinition = customRole
        override val roleOverride: RoleOverride = o
      }

      val checking: PermissionsChecking = mock[PermissionsChecking]
      command.permissionsCheck(checking)

      verify(checking, times(1)).PermissionCheck(Permissions.RolesAndPermissions.ManageCustomRoles, roleOverride)
    }
  }

  @Test(expected = classOf[ItemNotFoundException]) def noDepartment(): Unit = {
    new Fixture {
      val o: RoleOverride = roleOverride

      val command: DeleteCustomRoleOverrideCommandPermissions with DeleteCustomRoleOverrideCommandState = new DeleteCustomRoleOverrideCommandPermissions with DeleteCustomRoleOverrideCommandState {
        override val department: Department = null
        override val customRoleDefinition: CustomRoleDefinition = customRole
        override val roleOverride: RoleOverride = o
      }

      val checking: PermissionsChecking = mock[PermissionsChecking]
      command.permissionsCheck(checking)
    }
  }

  private trait ValidationFixture extends Fixture {
    val d: Department = department
    val o: RoleOverride = roleOverride

    val command: DeleteCustomRoleOverrideCommandValidation with CommandTestSupport = new DeleteCustomRoleOverrideCommandValidation with CommandTestSupport {
      val department: Department = d
      val customRoleDefinition: CustomRoleDefinition = customRole
      val roleOverride: RoleOverride = o
    }
  }

  @Test def validateNoErrors(): Unit = withUser("cuscav") {
    new ValidationFixture {
      command.permissionsService.getAllGrantedRolesForDefinition(customRole) returns Nil
      command.permissionsService.getCustomRoleDefinitionsBasedOn(customRole) returns Nil

      val errors = new BindException(command, "command")
      command.validate(errors)

      errors.hasErrors should be(false)
    }
  }

  @Test def validateCantRevokeWhatYouDontHave(): Unit = withUser("cuscav") {
    new ValidationFixture {
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
      errors.getGlobalError.getCodes should contain("permissions.cantRevokeWhatYouDontHave")
    }
  }

  @Test def description(): Unit = {
    new Fixture {
      val dept: Department = department
      val o: RoleOverride = roleOverride

      val command: DeleteCustomRoleOverrideCommandDescription with DeleteCustomRoleOverrideCommandState = new DeleteCustomRoleOverrideCommandDescription with DeleteCustomRoleOverrideCommandState {
        override val eventName: String = "test"
        val department: Department = dept
        val customRoleDefinition: CustomRoleDefinition = customRole
        val roleOverride: RoleOverride = o
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
