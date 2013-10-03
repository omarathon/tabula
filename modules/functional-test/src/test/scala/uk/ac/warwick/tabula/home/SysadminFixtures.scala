package uk.ac.warwick.tabula.home

import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.LoginDetails

trait SysadminFixtures extends BrowserTest {

	before {
		go to (Path("/scheduling/fixtures/setup"))
	}

	def as[T](user: LoginDetails)(fn: => T) = {
		currentUser = user
		signIn as(user) to (Path("/sysadmin"))
		fn
	}

	def withGodModeEnabled[T](fn: =>T)={
		go to (Path("/sysadmin"))
		click on id("enable-godmode-button")
		// if it wasn't found, perhaps god-mode is already enabled. Either way, check that it's enabled now.
		eventually(pageSource.contains("God mode enabled") should be (true))
		fn
	}
}