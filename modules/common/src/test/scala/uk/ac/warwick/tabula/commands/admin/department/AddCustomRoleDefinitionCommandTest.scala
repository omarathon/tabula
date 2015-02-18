package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.{ItemNotFoundException, Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.DescriptionImpl

class AddCustomRoleDefinitionCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends AddCustomRoleDefinitionCommandState with PermissionsServiceComponent {
		val permissionsService = mock[PermissionsService]
	}

	private trait Fixture {
		val department = Fixtures.department("in")
	}

	private trait CommandFixture extends Fixture {
		val command = new AddCustomRoleDefinitionCommandInternal(department) with CommandTestSupport
	}

	@Test def apply { new CommandFixture {
		command.name = "Custom role"
		command.baseDefinition = DepartmentalAdministratorRoleDefinition

		val created = command.applyInternal()
		created.name should be ("Custom role")
		created.baseRoleDefinition should be (DepartmentalAdministratorRoleDefinition)
		created.isAssignable should be (true)
		created.canDelegateThisRolesPermissions.booleanValue() should be (true)
		created.department should be (department)

		there was one (command.permissionsService).saveOrUpdate(created)
	}}

	@Test def permissions {
		val command = new AddCustomRoleDefinitionCommandPermissions with AddCustomRoleDefinitionCommandState {
			override val department = Fixtures.department("in")
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		there was one(checking).PermissionCheck(Permissions.RolesAndPermissions.Create, command.department)
	}

	@Test(expected = classOf[ItemNotFoundException]) def noDepartment {
		val command = new AddCustomRoleDefinitionCommandPermissions with AddCustomRoleDefinitionCommandState {
			override val department = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture {
		val command = new AddCustomRoleDefinitionCommandValidation with CommandTestSupport {
			val department = Fixtures.department("in")
		}
	}

	@Test def validateNoErrors { new ValidationFixture {
		command.name = "Custom role"
		command.baseDefinition = DepartmentalAdministratorRoleDefinition

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateNoName { new ValidationFixture {
		command.name = "         "
		command.baseDefinition = DepartmentalAdministratorRoleDefinition

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("customRoleDefinition.name.empty")
	}}

	@Test def validateNameTooLong { new ValidationFixture {
		command.name = (1 to 300).map { _ => "a" }.mkString("")
		command.baseDefinition = DepartmentalAdministratorRoleDefinition

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("customRoleDefinition.name.tooLong")
	}}

	@Test def validateNoBaseDefinition { new ValidationFixture {
		command.name = "Custom role"

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("baseDefinition")
		errors.getFieldError.getCodes should contain ("NotEmpty")
	}}

	@Test def description {
		val command = new AddCustomRoleDefinitionCommandDescription with AddCustomRoleDefinitionCommandState {
			override val eventName: String = "test"
			val department = Fixtures.department("in")
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in"
		))
	}

}
