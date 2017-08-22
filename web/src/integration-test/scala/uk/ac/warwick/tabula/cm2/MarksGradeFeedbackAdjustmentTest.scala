package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.Select
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

class MarksGradeFeedbackAdjustmentTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	private def openAssignmentsScreen(): Unit = {
		When("I go the admin page")
		click on linkText("Test Services")
		Then("I should be able to click on the Assignments dropdown")
		val toolbar = findAll(className("dept-toolbar")).next().underlying
		click on toolbar.findElement(By.partialLinkText("Assignments"))
		And("I should see the create assignments from SITS in the assignments menu option")
		val createAssignmentsLink = toolbar.findElement(By.partialLinkText("Create assignments from SITS"))
		eventually(timeout(45.seconds), interval(300.millis)) ({
			createAssignmentsLink.isDisplayed should be {
				true
			}
		})
		When("I click the create assignments from SITS link")
		click on createAssignmentsLink
		eventually(timeout(45.seconds), interval(300.millis)) ({
			Then("I should reach the create assignments from previous page")
			currentUrl should include("/2017/setup-assignments")
		})
	}

	private def changeMarksGrades(): Unit = {

	}

	"Department admin" should "be able to adjust marks and grades for assignment" in as(P.Admin1) {
		openAssignmentsScreen()
		changeMarksGrades()
	}
}
