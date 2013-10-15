package uk.ac.warwick.tabula.attendance

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.FunctionalTestAcademicYear

class AttendanceHomePageTest extends AttendanceFixture with GivenWhenThen{

	val sprCode = P.Student1.warwickId+"_1"
	val year = FunctionalTestAcademicYear.current.startYear

	"A student" should "see monitoring points for the current year" in {

		Given("I am logged in as Student1")
		signIn as P.Student1 to Path("/")

		When("I go to /attendance")
		go to Path("/attendance")

		Then("I am redirected to /attendance/{spr-code}/{current-year}")
		eventually(currentUrl should include(s"/attendance/profile/$sprCode/$year"))
		pageSource should include("My Attendance Monitoring")
	}

	"A Member of staff" should "see the monitoring points home page" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		When("I go to /attendance")
		go to Path("/attendance")

		Then("I see the attendance admin sections")
		pageSource should include("View and record monitoring points")
		pageSource should include("Create and edit monitoring schemes")

		findAll(id(s"view-department-$TEST_DEPARTMENT_CODE")).toList.size should be (1)
		findAll(id(s"manage-department-$TEST_DEPARTMENT_CODE")).toList.size should be (1)
	}

	"A member of staff with a relationship" should "see the monitoring points home page" in {
		Given("I am logged in as Marker1")
		signIn as P.Marker1 to Path("/")

		And("Marker 1 is tutor to Student 1")
		createStudentRelationship(P.Student1,P.Marker1)

		When("I go to /attendance")
		go to Path("/attendance")

		Then("I see the personal tutor section")

		pageSource should include("My students")

		findAll(id("relationship-tutor")).toList.size should be (1)

		pageSource should not include("View and record monitoring points")
		pageSource should not include("Create and edit monitoring schemes")

		findAll(id(s"view-department-$TEST_DEPARTMENT_CODE")).toList.size should be (0)
		findAll(id(s"manage-department-$TEST_DEPARTMENT_CODE")).toList.size should be (0)
	}

}
