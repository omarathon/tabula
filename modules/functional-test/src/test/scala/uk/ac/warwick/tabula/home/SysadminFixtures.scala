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
}