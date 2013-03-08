package uk.ac.warwick.tabula.permissions

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.TestBase

class PermissionTest extends TestBase {

	@Test def of {		
		Permissions.of("Module.Read") match {
			case Module.Read => 
			case what:Any => fail("what is this?" + what) 
		}
	}
	
	@Test(expected=classOf[IllegalArgumentException]) def invalidAction {
		Permissions.of("Spank")
	}
	
	@Test(expected=classOf[IllegalArgumentException]) def obsoleteProfilesRead {
		// this perm made obsolete (as leaf node) in TAB-564
		Permissions.of("Profiles.Read")
	}
	
	@Test def name {
		Permissions.Assignment.Archive.getName should be ("Assignment.Archive")
		Permissions.GodMode.getName should be ("GodMode")
		Permissions.of("Module.Read").getName should be ("Module.Read")
	}
	
}