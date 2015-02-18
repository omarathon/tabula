package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.{ItemNotFoundException, Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.data.model.permissions.{RoleOverride, CustomRoleDefinition}
import scala.collection.JavaConverters._

class DeleteCustomRoleOverrideCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends DeleteCustomRoleOverrideCommandState with PermissionsServiceComponent {
		val permissionsService = mock[PermissionsService]
	}

	private trait Fixture {
		val department = Fixtures.department("in")

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

	@Test def init { new CommandFixture {
		command.department should be (department)
		command.customRoleDefinition should be (customRole)
		command.roleOverride should be (roleOverride)
	}}

	@Test def apply { new CommandFixture {
		command.applyInternal() should be (roleOverride)
		customRole.overrides.asScala should be ('empty)

		there was one (command.permissionsService).saveOrUpdate(customRole)
	}}

	@Test def permissions { new Fixture {
		val d = department
		val o = roleOverride

		val command = new DeleteCustomRoleOverrideCommandPermissions with DeleteCustomRoleOverrideCommandState {
			override val department = d
			override val customRoleDefinition = customRole
			override val roleOverride = o
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		there was one(checking).PermissionCheck(Permissions.RolesAndPermissions.Delete, roleOverride)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def noDepartment { new Fixture {
		val o = roleOverride

		val command = new DeleteCustomRoleOverrideCommandPermissions with DeleteCustomRoleOverrideCommandState {
			override val department = null
			override val customRoleDefinition = customRole
			override val roleOverride = o
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}}

	private trait ValidationFixture extends Fixture {
		val d = department
		val o = roleOverride

		val command = new DeleteCustomRoleOverrideCommandValidation with CommandTestSupport {
			val department = d
			val customRoleDefinition = customRole
			val roleOverride = o
		}
	}

	@Test def validateNoErrors { new ValidationFixture {
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def description { new Fixture {
		val dept = department
		val o = roleOverride

		val command = new DeleteCustomRoleOverrideCommandDescription with DeleteCustomRoleOverrideCommandState {
			override val eventName: String = "test"
			val department = dept
			val customRoleDefinition = customRole
			val roleOverride = o
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"customRoleDefinition" -> "custom"
		))
	}}

}
