package uk.ac.warwick.tabula.attendance.manage

import org.joda.time.DateTime
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.FunctionalTestAcademicYear
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceEditStudentsTest extends AttendanceFixture with GivenWhenThen {

	val thisAcademicYearString = new FunctionalTestAcademicYear(new DateTime().getYear).startYear.toString

	"A Member of staff" should "be able to edit students on a scheme" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		// HTMLUnit javascript messes up the DOM when you have use-tooltip on a form element you want to query for
		// Without disabling js, it's impossible to click on the submit button
		ifHtmlUnitDriver(h=>h.setJavascriptEnabled(false))

		When(s"I go to /attendance/manage/xxx/$thisAcademicYearString")
		go to Path(s"/attendance/manage/xxx/$thisAcademicYearString")

		And("I choose to edit the students on a scheme")
		click on linkText("1 student")

		Then("I see the students currently on the scheme")
		eventually(currentUrl should endWith(s"students"))
		pageSource should include("1 students on this scheme")
		pageSource should include("0 from SITS")

		When("I add a student manually")
		click on cssSelector("input[name=manuallyAddForm]")
		eventually(pageSource should include("Add students manually"))
		click on cssSelector("textarea[name=massAddUsers]")
		pressKeys("tabula-functest-student2")
		click on cssSelector("#main-content form input.btn.btn-success")

		Then("I see the manually added student")
		eventually(
			findAll(cssSelector("details.manually-added table.manage-student-table tbody tr")).size should be (2)
		)
		pageSource should include("2 students on this scheme")
		pageSource should include("(0 from SITS, plus 2 added manually)")

		When("I choose a route")
		click on cssSelector("#main-content input[name=routes]")

		And("I click on Find")
		click on cssSelector("#main-content button[name=findStudents]")

		Then("I see the students")
		eventually(
			findAll(cssSelector("details.find-students table.manage-student-table tbody tr")).size should be (2)
		)
		pageSource should include("3 students on this scheme")
		pageSource should include("(1 from SITS, plus 2 added manually)")

		When("I choose to link to SITS")
		click on cssSelector("input[name=linkToSits]")
		And("I save the scheme")
		click on cssSelector("#main-content form input.btn.btn-primary")

		Then("I am redirected to the manage home page")
		eventually(currentUrl should endWith(s"/attendance/manage/xxx/$thisAcademicYearString"))
		pageSource should include(s"Manage monitoring points for ${new FunctionalTestAcademicYear(new DateTime().getYear).toString}")

		When("I choose to edit the students on the same scheme")
		click on linkText("3 students")

		Then("I see the students currently on the scheme")
		eventually(currentUrl should endWith(s"students"))
		pageSource should include("3 students on this scheme")

		When("I reset both manually added students")
		cssSelector("details.manually-added input[name=resetStudentIds]").findAllElements.foreach(input => click on input)
		click on cssSelector("input[name=resetMembership]")

		Then("Only the SITS students remain")
		eventually {
			findAll(cssSelector("details.find-students table.manage-student-table tbody tr")).size should be(2)
			findAll(cssSelector("details.manually-added table.manage-student-table tbody tr")).size should be(0)
		}
		pageSource should include("2 students on this scheme")

		When("I exclude the SITS students")
		cssSelector("details.find-students input[name=excludeIds]").findAllElements.foreach(input => click on input)
		click on cssSelector("input[name=manuallyExclude]")

		Then("No students remain")
		eventually {
			findAll(cssSelector("details.find-students table.manage-student-table tbody tr")).size should be(2)
			findAll(cssSelector("details.find-students table.manage-student-table tbody tr.exclude")).size should be(2)
			findAll(cssSelector("details.manually-added table.manage-student-table tbody tr")).size should be(2)
		}

		When("I save the scheme")
		click on cssSelector("#main-content form input.btn.btn-primary")

		Then("I am redirected to the manage home page")
		eventually(currentUrl should endWith(s"/attendance/manage/xxx/$thisAcademicYearString"))
		pageSource should include(s"Manage monitoring points for ${new FunctionalTestAcademicYear(new DateTime().getYear).toString}")
		pageSource should include("0 students")

	}
}
