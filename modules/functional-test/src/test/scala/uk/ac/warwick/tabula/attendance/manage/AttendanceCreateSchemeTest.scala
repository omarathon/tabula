package uk.ac.warwick.tabula.attendance.manage

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceCreateSchemeTest extends AttendanceFixture with GivenWhenThen {

	val schemeName = "The Scheme of things"

	"A Member of staff" should "be able to create monitoring point schemes" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		// HTMLUnit javascript messes up the DOM when you have use-tooltip on a form element you want to query for
		// Without disabling js, it's impossible to click on the submit button
		ifHtmlUnitDriver(h=>h.setJavascriptEnabled(false))

		When("I go to /attendance/manage/xxx/2014/new")
		go to Path("/attendance/manage/xxx/2014/new")

		Then("I can enter a scheme name")
		click on id("name")
		pressKeys(schemeName)

		When("then I select 'term weeks' as the Date format")
		radioButtonGroup("pointStyle").value= "week"

		And("I can create the scheme")
		click on cssSelector("#main-content form input.btn.btn-primary")

		Then("I am redirected to the manage home page")
		eventually(currentUrl should endWith("/attendance/manage/xxx/2014"))
		pageSource should include("Manage monitoring points for 14/15")
		pageSource should include(schemeName)

		When("The I click the 'Add points' link")
		click on (linkText("Add points"))

		Then("I am redirected to the add points page")
		eventually(currentUrl should include("/attendance/manage/xxx/2014/addpoints"))
		pageSource should include(schemeName)

	}
}
