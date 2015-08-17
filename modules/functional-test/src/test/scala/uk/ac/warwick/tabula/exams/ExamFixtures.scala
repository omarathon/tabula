package uk.ac.warwick.tabula.exams

import uk.ac.warwick.tabula.{BrowserTest, LoginDetails}


class ExamFixtures extends BrowserTest {

	before {
		go to Path("/scheduling/fixtures/setup")
		pageSource should include("Fixture setup successful")
	}

	def as[T](user: LoginDetails)(fn: => T) = {
		currentUser = user
		signIn as user to Path("/exams")

		fn
	}
}
