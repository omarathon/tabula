package uk.ac.warwick.tabula.permissions

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.TestBase

class PermissionTest extends TestBase {

	@Test def of {		
		Permissions.of("Module.Read") match {
			case Module.Read() => 
			case what:Any => fail("what is this?" + what) 
		}
	}
	
	@Test(expected=classOf[IllegalArgumentException]) def invalidAction {
		Permissions.of("Spank")
	}
	
}