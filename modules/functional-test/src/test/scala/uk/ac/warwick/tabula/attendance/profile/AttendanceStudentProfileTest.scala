package uk.ac.warwick.tabula.attendance.profile

import org.scalatest.GivenWhenThen
import org.openqa.selenium.By
import uk.ac.warwick.tabula.attendance.AttendanceFixture
import org.scalatest.selenium.WebBrowser.go

class AttendanceStudentProfileTest extends AttendanceFixture with GivenWhenThen {

	"A Student" should "see their profile" in {
		Given("I am logged in as Student1")
		signIn as P.Student1 to Path("/")

		When("I go to /attendance/")
		go to Path("/attendance/")

		Then("I am redirected to my profile")
		eventually(currentUrl should include("/attendance/profile"))
		pageSource should include("My Attendance Monitoring")
		And("I see my monitoring points")
		className("monitoring-points-profile").webElement.findElements(By.cssSelector("tr.point")).size() should be (3)
	}

}
