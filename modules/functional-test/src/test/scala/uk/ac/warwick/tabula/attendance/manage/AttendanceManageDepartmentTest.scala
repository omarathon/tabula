package uk.ac.warwick.tabula.attendance.manage

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.FunctionalTestAcademicYear
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceManageDepartmentTest extends AttendanceFixture with GivenWhenThen {

	"A Member of staff" should "see a link for their department for the current academic year" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		When(s"I go to /attendance/manage/xxx")
		go to Path(s"/attendance/manage/xxx")

		And("I click the link to manage xxx for this academic year")
		click on linkText(s"Test Services ${FunctionalTestAcademicYear.current.toString}")

		Then("I am redirected to the manage department for year page")
		eventually(currentUrl should include(s"/attendance/manage/xxx/${FunctionalTestAcademicYear.current.startYear.toString}"))

	}
}
