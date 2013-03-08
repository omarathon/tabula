package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.roles.Sysadmin
import uk.ac.warwick.tabula.Fixtures

class SysadminRoleProviderTest extends TestBase {
	
	val provider = new SysadminRoleProvider
	
	val userLookup = new MockUserLookup
	provider.userLookup = userLookup
	provider.adminGroup = "tabula-sysadmins"
		
	userLookup.groupService.usersInGroup ++= Map(
		("cuscav", "othergroup") -> true,
		("cuscav", "tabula-sysadmins") -> true
	)
	
	@Test def itWorks {
		withUser("cuscav") {
			provider.getRolesFor(currentUser) should be (Seq(Sysadmin()))
		}
		
		withUser("cusebr") {
			provider.getRolesFor(currentUser) should be (Seq())
		}
	}

}