package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import uk.ac.warwick.tabula.TestBase

class PermissionsHelperControllerTest extends TestBase {
	
	@Test def allPermissions() = {
		println(new PermissionsHelperController().allPermissions)
		println(new PermissionsHelperController().allPermissionTargets)
	}

}