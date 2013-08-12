package uk.ac.warwick.tabula.admin

import scala.collection.JavaConverters._
import org.joda.time.DateTime
import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.LoginDetails

trait AdminFixtures extends BrowserTest {

	before {
		go to (Path("/scheduling/fixtures/setup"))
	}

	def as[T](user: LoginDetails)(fn: => T) = {
		currentUser = user
		signIn as(user) to (Path("/admin"))

		fn
	}
}