package uk.ac.warwick.tabula.commands.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.permissions.{PermissionsServiceComponent, PermissionsService}
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{SecurityServiceComponent, SecurityService}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.commands.{Describable, SelfValidating, Appliable, DescriptionImpl}

class GrantPermissionsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport[A <: PermissionsTarget] extends GrantPermissionsCommandState[A] with PermissionsServiceComponent with SecurityServiceComponent {
		val permissionsService = mock[PermissionsService]
		val securityService = mock[SecurityService]
	}

	trait Fixture {
		val department = Fixtures.department("in", "IT Services")

		val command = new GrantPermissionsCommandInternal(department) with CommandTestSupport[Department] with GrantPermissionsCommandValidation
	}
	
	@Test def itWorksForNewPermission { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.overrideType = GrantedPermission.Allow

		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (None)
				
		val grantedPerm = command.applyInternal()
		grantedPerm.permission should be (Permissions.Department.ManageExtensionSettings)
		grantedPerm.users.size should be (2)
		grantedPerm.users.knownType.includesUserId("cuscav") should be (true)
		grantedPerm.users.knownType.includesUserId("cusebr") should be (true)
		grantedPerm.users.knownType.includesUserId("cuscao") should be (false)
		grantedPerm.overrideType should be (GrantedPermission.Allow)
		grantedPerm.scope should be (department)
	}}
	
	@Test def itWorksWithExisting { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.overrideType = GrantedPermission.Allow
		
		val existing = GrantedPermission(department, Permissions.Department.ManageExtensionSettings, true)
		existing.users.knownType.addUserId("cuscao")

		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (Some(existing))
				
		val grantedPerm = command.applyInternal()
		(grantedPerm.eq(existing)) should be (true)
		
		grantedPerm.permission should be (Permissions.Department.ManageExtensionSettings)
		grantedPerm.users.size should be (3)
		grantedPerm.users.knownType.includesUserId("cuscav") should be (true)
		grantedPerm.users.knownType.includesUserId("cusebr") should be (true)
		grantedPerm.users.knownType.includesUserId("cuscao") should be (true)
		grantedPerm.overrideType should be (GrantedPermission.Allow)
		grantedPerm.scope should be (department)
	}}
	
	@Test def validatePasses { withUser("cuscav", "0672089") { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.overrideType = GrantedPermission.Allow

		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (None)
		command.securityService.canDelegate(currentUser, Permissions.Department.ManageExtensionSettings, department) returns (true)
		
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}}
	
	@Test def noUsercodes { withUser("cuscav", "0672089") { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.overrideType = GrantedPermission.Allow

		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (None)
		command.securityService.canDelegate(currentUser, Permissions.Department.ManageExtensionSettings, department) returns (true)
		
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}}
	
	@Test def duplicateUsercode { withUser("cuscav", "0672089") { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.usercodes.add("cuscao")
		command.overrideType = GrantedPermission.Allow
		
		val existing = GrantedPermission(department, Permissions.Department.ManageExtensionSettings, true)
		existing.users.knownType.addUserId("cuscao")

		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (Some(existing))
		command.securityService.canDelegate(currentUser, Permissions.Department.ManageExtensionSettings, department) returns (true)
		
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("userId.duplicate")
	}}}
	
	@Test def noPermission { withUser("cuscav", "0672089") { new Fixture {
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.overrideType = GrantedPermission.Allow

		command.permissionsService.getGrantedPermission(department, null, true) returns (None)
		
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("permission")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}}
	
	@Test def cantGiveWhatYouDontHave { withUser("cuscav", "0672089") { new Fixture {
		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")
		command.overrideType = GrantedPermission.Allow
		
		command.permissionsService.getGrantedPermission(department, Permissions.Department.ManageExtensionSettings, true) returns (None)
		command.securityService.canDelegate(currentUser, Permissions.Department.ManageExtensionSettings, department) returns (false)
		
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("permission")
		errors.getFieldError.getCode should be ("permissions.cantGiveWhatYouDontHave")
	}}}

	@Test
	def describe {
		val department = Fixtures.department("in")
		department.id = "department-id"

		val command = new GrantPermissionsCommandDescription[Department] with CommandTestSupport[Department] {
			val eventName: String = "test"

			val scope = department
			val grantedPermission = None
		}

		command.permission = Permissions.Department.ManageExtensionSettings
		command.usercodes.add("cuscav")
		command.usercodes.add("cusebr")

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"scope" -> "Department[department-id]",
			"usercodes" -> "cuscav,cusebr",
			"permission" -> "Department.ManageExtensionSettings",
			"overrideType" -> false
		))
	}

	@Test def gluesEverythingTogether {
		val department = Fixtures.department("in")
		val command = GrantPermissionsCommand(department)

		command should be (anInstanceOf[Appliable[GrantedPermission[Department]]])
		command should be (anInstanceOf[GrantPermissionsCommandState[Department]])
		command should be (anInstanceOf[GrantPermissionsCommandPermissions])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[GrantPermissionsCommandValidation])
		command should be (anInstanceOf[Describable[GrantedPermission[Department]]])
	}

}