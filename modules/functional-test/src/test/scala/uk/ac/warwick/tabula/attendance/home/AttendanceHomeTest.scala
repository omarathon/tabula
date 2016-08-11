package uk.ac.warwick.tabula.attendance.home

import org.openqa.selenium.By
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.FunctionalTestAcademicYear
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceHomeTest extends AttendanceFixture with GivenWhenThen{

	val year = FunctionalTestAcademicYear.current.startYear
	val yearSITS = FunctionalTestAcademicYear.currentSITS.startYear

	"A student" should "see monitoring points for the current year" in {
		Given("I am logged in as Student1")
		signIn as P.Student1 to Path("/")

		When("I go to /attendance")
		go to Path("/attendance")

		Then(s"I am redirected to /attendance/${P.Student1.warwickId}/$year")
		eventually(currentUrl should include(s"/attendance/profile/${P.Student1.warwickId}/$year"))
		pageSource should include("My Monitoring Points")
	}

	"A Member of staff" should "see the monitoring points home page" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		When("I go to /attendance")
		go to Path("/attendance")
		openDisplayAcademicYearSettings()

		Then("I see the attendance admin sections")
		pageSource should include("View and record monitoring points")
		pageSource should include("Create and edit monitoring schemes")

		And("I should see current academic year")
		findAll(id(s"view-department-$TEST_DEPARTMENT_CODE-$yearSITS")).toList.size should be (1)
		findAll(id(s"manage-department-$TEST_DEPARTMENT_CODE-$yearSITS")).toList.size should be (1)
	}

	def openDisplayAcademicYearSettings() = {
		eventually {
			find(cssSelector(".dept-settings a.dropdown-toggle")) should be('defined)
		}
		click on cssSelector(".dept-settings a.dropdown-toggle")
		val displayLink = cssSelector(".dept-settings .dropdown-menu").webElement.findElement(By.partialLinkText(s"${FunctionalTestAcademicYear.currentSITS.toString}"))
		eventually {
			displayLink.isDisplayed should be {true}
		}
		click on displayLink
	}

	"A member of staff with a relationship" should "see the monitoring points home page" in {
		Given("I am logged in as Marker1")
		signIn as P.Marker1 to Path("/")

		And("Marker 1 is tutor to Student 1")
		createStudentRelationship(P.Student1, P.Marker1)

		When("I go to /attendance")
		go to Path("/attendance")

		Then("I see the personal tutor section")

		pageSource should include("My students")

		findAll(id(s"relationship-tutor-$yearSITS")).toList.size should be (1)

		pageSource should not include "View and record monitoring points"
		pageSource should not include "Create and edit monitoring schemes"

		findAll(id(s"view-department-$TEST_DEPARTMENT_CODE-$yearSITS")).toList.size should be (0)
		findAll(id(s"manage-department-$TEST_DEPARTMENT_CODE-$yearSITS")).toList.size should be (0)
	}

}
