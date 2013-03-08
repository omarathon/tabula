package uk.ac.warwick.tabula.home.commands.admin

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.CurrentUser

class MasqueradeCommandTest extends TestBase {
	
	val userLookup = new MockUserLookup
	userLookup.registerUsers("cusebr")
	
	@Test def set {
		val cmd = new MasqueradeCommand
		cmd.userLookup = userLookup
		
		cmd.usercode = "cusebr"
			
		val cookie = cmd.applyInternal
		cookie should be ('defined)
		cookie.map { cookie =>
			cookie.cookie.getName() should be (CurrentUser.masqueradeCookie)
			cookie.cookie.getValue() should be ("cusebr")
			cookie.cookie.getPath() should be ("/")
		}
	}
	
	@Test def setInvalidUser {
		val cmd = new MasqueradeCommand
		cmd.userLookup = userLookup
		
		cmd.usercode = "undefined"
			
		val cookie = cmd.applyInternal
		cookie should be ('empty)
	}
	
	@Test def remove {
		val cmd = new MasqueradeCommand
		cmd.userLookup = userLookup
		
		cmd.action = "remove"
			
		val cookie = cmd.applyInternal
		cookie should be ('defined)
		cookie.map { cookie =>
			cookie.cookie.getName() should be (CurrentUser.masqueradeCookie)
			cookie.cookie.getValue() should be (null) // removal
			cookie.cookie.getPath() should be ("/")
		}
	}

}