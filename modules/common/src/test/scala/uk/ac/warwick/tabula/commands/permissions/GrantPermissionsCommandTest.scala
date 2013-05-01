package uk.ac.warwick.tabula.commands.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.SecurityService
import scala.reflect.ClassTag

class GrantPermissionsCommandTest extends TestBase with Mockito {
	
	val permissionsService = mock[PermissionsService]
	val securityService = mock[SecurityService]
	
	private def command[A <: PermissionsTarget: ClassTag](scope: A) = {
		val cmd = new GrantPermissionsCommand(scope)
		cmd.permissionsService = permissionsService
		cmd.securityService = securityService
		
		cmd
	}
	
	@Test def itWorksForNewPermission {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.permission = Permissions.Department.ManageExtensionSettings
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		cmd.overrideType = GrantedPermission.Allow
		
		permissionsService.getGrantedPermission(dept, Permissions.Department.ManageExtensionSettings, true) returns (None)
				
		val grantedPerm = cmd.applyInternal()
		grantedPerm.permission should be (Permissions.Department.ManageExtensionSettings)
		grantedPerm.users.includeUsers.size() should be (2)
		grantedPerm.users.includes("cuscav") should be (true)
		grantedPerm.users.includes("cusebr") should be (true)
		grantedPerm.users.includes("cuscao") should be (false)
		grantedPerm.overrideType should be (GrantedPermission.Allow)
		grantedPerm.scope should be (dept)
	}
	
	@Test def itWorksWithExisting {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.permission = Permissions.Department.ManageExtensionSettings
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		cmd.overrideType = GrantedPermission.Allow
		
		val existing = GrantedPermission(dept, Permissions.Department.ManageExtensionSettings, true)
		existing.users.addUser("cuscao")
		
		permissionsService.getGrantedPermission(dept, Permissions.Department.ManageExtensionSettings, true) returns (Some(existing))
				
		val grantedPerm = cmd.applyInternal()
		(grantedPerm.eq(existing)) should be (true)
		
		grantedPerm.permission should be (Permissions.Department.ManageExtensionSettings)
		grantedPerm.users.includeUsers.size() should be (3)
		grantedPerm.users.includes("cuscav") should be (true)
		grantedPerm.users.includes("cusebr") should be (true)
		grantedPerm.users.includes("cuscao") should be (true)
		grantedPerm.overrideType should be (GrantedPermission.Allow)
		grantedPerm.scope should be (dept)
	}
	
	@Test def validatePasses { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.permission = Permissions.Department.ManageExtensionSettings
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		cmd.overrideType = GrantedPermission.Allow
		
		permissionsService.getGrantedPermission(dept, Permissions.Department.ManageExtensionSettings, true) returns (None)
		securityService.can(currentUser, Permissions.Department.ManageExtensionSettings, dept) returns (true)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (false)
	}}
	
	@Test def noUsercodes { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.permission = Permissions.Department.ManageExtensionSettings
		cmd.overrideType = GrantedPermission.Allow
		
		permissionsService.getGrantedPermission(dept, Permissions.Department.ManageExtensionSettings, true) returns (None)
		securityService.can(currentUser, Permissions.Department.ManageExtensionSettings, dept) returns (true)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}
	
	@Test def duplicateUsercode { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.permission = Permissions.Department.ManageExtensionSettings
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		cmd.usercodes.add("cuscao")
		cmd.overrideType = GrantedPermission.Allow
		
		val existing = GrantedPermission(dept, Permissions.Department.ManageExtensionSettings, true)
		existing.users.addUser("cuscao")
		
		permissionsService.getGrantedPermission(dept, Permissions.Department.ManageExtensionSettings, true) returns (Some(existing))
		securityService.can(currentUser, Permissions.Department.ManageExtensionSettings, dept) returns (true)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("usercodes")
		errors.getFieldError.getCode should be ("userId.duplicate")
	}}
	
	@Test def noPermission { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		cmd.overrideType = GrantedPermission.Allow
		
		permissionsService.getGrantedPermission(dept, null, true) returns (None)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("permission")
		errors.getFieldError.getCode should be ("NotEmpty")
	}}
	
	@Test def cantGiveWhatYouDontHave { withUser("cuscav", "0672089") {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = command(dept)
		cmd.permission = Permissions.Department.ManageExtensionSettings
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		cmd.overrideType = GrantedPermission.Allow
		
		permissionsService.getGrantedPermission(dept, Permissions.Department.ManageExtensionSettings, true) returns (None)
		securityService.can(currentUser, Permissions.Department.ManageExtensionSettings, dept) returns (false)
		
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("permission")
		errors.getFieldError.getCode should be ("permissions.cantGiveWhatYouDontHave")
	}}

}