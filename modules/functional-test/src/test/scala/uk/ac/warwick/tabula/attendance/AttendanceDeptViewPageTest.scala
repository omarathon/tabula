package uk.ac.warwick.tabula.attendance

import org.scalatest.GivenWhenThen

class AttendanceDeptViewPageTest extends AttendanceFixture with GivenWhenThen{

	"A Member of staff" should "see the department View page" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		When("I go to /attendance/xxx")
		go to Path("/attendance/xxx")

		Then("I see the attendance view links")
		pageSource should include("View by student")
		pageSource should include("View by point")
		pageSource should include("View by personal tutor")
		pageSource should include("View by supervisor")
	}

}
