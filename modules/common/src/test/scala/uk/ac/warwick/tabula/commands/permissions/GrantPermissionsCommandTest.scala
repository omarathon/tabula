package uk.ac.warwick.tabula.commands.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission

class GrantPermissionsCommandTest extends TestBase with Mockito {
	
	val permissionsService = mock[PermissionsService]
	
	@Test def itWorksForNewPermission {
		val dept = Fixtures.department("in", "IT Services")
		
		val cmd = new GrantPermissionsCommand(dept)
		cmd.permission = Permissions.Department.ManageExtensionSettings
		cmd.usercodes.add("cuscav")
		cmd.usercodes.add("cusebr")
		cmd.overrideType = GrantedPermission.Allow
		
		permissionsService.getGrantedPermission(dept, Permissions.Department.ManageExtensionSettings, true) returns (None)
		
		cmd.permissionsService = permissionsService
		
		val grantedPerm = cmd.applyInternal()
		grantedPerm.permission should be (Permissions.Department.ManageExtensionSettings)
		grantedPerm.users.includeUsers.size() should be (2)
		grantedPerm.users.includes("cuscav") should be (true)
		grantedPerm.users.includes("cusebr") should be (true)
		grantedPerm.overrideType should be (GrantedPermission.Allow)
		grantedPerm.scope should be (dept)
	}

}