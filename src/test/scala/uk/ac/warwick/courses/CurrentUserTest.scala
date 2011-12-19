package uk.ac.warwick.courses

import org.junit.Test
import org.scalatest.Tracker
import uk.ac.warwick.userlookup.User

class CurrentUserTest extends TestBase {

	@Test def stringify {
		user("cusaaa", "cusaaa", false).toString should be ("User cusaaa")
		user("cusaaa", "cusaab", false).toString should be ("User cusaab (really cusaaa)")
		user("cusaaa", "cusaab", true).toString should be ("User cusaab (really cusaaa) +GodMode")
		user("cusaaa", "cusaaa", true).toString should be ("User cusaaa +GodMode")
	}
	
	private def user(code:String, masqCode:String, sysadEnabled:Boolean) = {
		new CurrentUser(new User(code), new User(masqCode), sysadmin=true, god=sysadEnabled)
	}
}