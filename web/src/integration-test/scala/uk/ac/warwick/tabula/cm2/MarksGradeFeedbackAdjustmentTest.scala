package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.Select
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

class MarksGradeFeedbackAdjustmentTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	private def openAssignmentsScreen(): Unit = {
		Given("There is an assignment with feedback")
		withAssignment("xxx01", "Fully featured assignment") { assignmentId =>

			When("I go the admin page")
			click on linkText("Test Services")

			// check that we can see some modules on the page.
			Then("Module XXX01 should be displayed")
			getModule("XXX01").get.isDisplayed should be {
				true
			}

			When("I click on the XXX01 module it should open")
			val module = getModule("XXX01").get
			click on module.findElement(By.className("mod-code"))

			Then("Fully featured assignment should be visible")
			linkText("Fully featured assignment").webElement.isDisplayed should be(true)

			click on linkText("Fully featured assignment")
			currentUrl should endWith(assignmentId+"/summary")

			click on partialLinkText("Feedback")

			And("I should see the Add marks menu option")
			val addMarksLink = partialLinkText("Add marks").webElement
			eventually {
				addMarksLink.isDisplayed should be {
					true
				}
			}
			When("I click the Add marks link")
			click on addMarksLink

			Then("I should reach the Submit marks and feedback settings page")
			currentUrl should include("/marks")
			And("Tab option Web Form should be available")
			linkText("Web Form").webElement.isDisplayed should be(true)

			click on linkText("Web Form")
			currentUrl should include("/marks#webform")
		}
	}

	private def addMarksGrades(): Unit = {

		var markPercentage = id("main").webElement.findElement(By.name("marks[0].actualMark"))
		markPercentage.sendKeys("68")

		var grade = id("main").webElement.findElement(By.name("marks[0].actualGrade"))
		grade.sendKeys("2.2")

		var feedbackComment = id("main").webElement.findElement(By.name("marks[0].feedbackComment"))
		feedbackComment.sendKeys("Interesting essay")

	}

	private def changeMarksGrades(): Unit = {}


	"Department admin" should "be able to adjust marks and grades for assignment" in as(P.Admin1) {

		openAssignmentsScreen()
		addMarksGrades()
		changeMarksGrades()
	}
}
