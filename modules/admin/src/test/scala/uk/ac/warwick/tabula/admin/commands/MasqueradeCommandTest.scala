package uk.ac.warwick.tabula.admin.commands

import uk.ac.warwick.tabula.{Fixtures, TestBase, MockUserLookup, CurrentUser}
import uk.ac.warwick.tabula.services.UserLookupComponent
import uk.ac.warwick.tabula.commands.{Describable, Appliable}
import uk.ac.warwick.tabula.data.model.{Route, Module}
import uk.ac.warwick.tabula.web.Cookie

class MasqueradeCommandTest extends TestBase {

	trait CommandTestSupport extends MasqueradeCommandState with UserLookupComponent {
		val userLookup = new MockUserLookup
		userLookup.registerUsers("cusebr")
	}

	trait Fixture {
		val command = new MasqueradeCommandInternal with CommandTestSupport
	}
	
	@Test def set { new Fixture {
		command.usercode = "cusebr"
			
		val cookie = command.applyInternal()
		cookie should be ('defined)
		cookie.map { cookie =>
			cookie.cookie.getName() should be (CurrentUser.masqueradeCookie)
			cookie.cookie.getValue() should be ("cusebr")
			cookie.cookie.getPath() should be ("/")
		}
	}}
	
	@Test def setInvalidUser { new Fixture {
		command.usercode = "undefined"
			
		val cookie = command.applyInternal()
		cookie should be ('empty)
	}}
	
	@Test def remove { new Fixture {
		command.action = "remove"
			
		val cookie = command.applyInternal()
		cookie should be ('defined)
		cookie.map { cookie =>
			cookie.cookie.getName() should be (CurrentUser.masqueradeCookie)
			cookie.cookie.getValue() should be (null) // removal
			cookie.cookie.getPath() should be ("/")
		}
	}}

	@Test
	def glueEverythingTogether() {
		val command = MasqueradeCommand()

		command should be (anInstanceOf[Appliable[Option[Cookie]]])
		command should be (anInstanceOf[MasqueradeCommandState])
		command should be (anInstanceOf[MasqueradeCommandPermissions])
		command should be (anInstanceOf[Describable[Option[Cookie]]])
	}

}