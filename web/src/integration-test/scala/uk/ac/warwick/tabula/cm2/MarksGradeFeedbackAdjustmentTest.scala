package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
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

		}
	}

	private def addMarksGrades(): Unit = {

		When("I click the Feedback menu section")
		click on partialLinkText("Feedback")

		Then("I should see the Add marks menu option")
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

		When("I create marks, grades and feedback")
		var markPercentage = id("main").webElement.findElement(By.name("marks[0].actualMark"))
		markPercentage.sendKeys("68")

		var grade = id("main").webElement.findElement(By.name("marks[0].actualGrade"))
		grade.sendKeys("2.2")

		var feedbackComment = id("main").webElement.findElement(By.name("marks[0].feedbackComment"))
		feedbackComment.sendKeys("Interesting essay")

		And("I save them")
		var saveBtn = id("main").webElement.findElements(By.cssSelector(".btn-primary")).get(1)
		click on saveBtn

		Then("The marks, grades and feedback should be received")
		id("main").webElement.findElement(By.cssSelector("form#command p")).getText should equal("I've received your files and I found marks and feedback for 1 students.")

		var confirmBtn = id("main").webElement.findElements(By.cssSelector(".btn-primary")).get(0)
		click on confirmBtn

		Then("The user assignemnt should be updated")
		id("main").webElement.findElements(By.cssSelector("td.action-col")).get(1).getText should equal("Feedback needs publishing")

	}

	private def changeMarksGrades(): Unit = {

		When("I click the Feedback menu section")
		click on partialLinkText("Feedback")

		Then("I should see the Add marks menu option")
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

		When("I create marks, grades and feedback")
		var markPercentage2 = id("main").webElement.findElement(By.name("marks[0].actualMark"))
		markPercentage2.clear()
		markPercentage2.sendKeys("71")

		var grade2 = id("main").webElement.findElement(By.name("marks[0].actualGrade"))
		grade2.clear()
		grade2.sendKeys("2.1")

		var feedbackComment2 = id("main").webElement.findElement(By.name("marks[0].feedbackComment"))
		feedbackComment2.clear()
		feedbackComment2.sendKeys("Very good")

		And("I save them")
		var saveBtn2 = id("main").webElement.findElements(By.cssSelector(".btn-primary")).get(1)
		click on saveBtn2

		Then("The marks, grades and feedback should be received")
		id("main").webElement.findElement(By.cssSelector("form#command p")).getText should equal("I've received your files and I found marks and feedback for 1 students.")

		And("The user should be warned this will replace the existing marks,grades and feedback")
		id("main").webElement.findElement(By.cssSelector("span.warning")).getText should equal("Feedback and/or marks have already been uploaded for this student. This will be overwritten when you click confirm")

		var confirmBtn2 = id("main").webElement.findElements(By.cssSelector(".btn-primary")).get(0)
		click on confirmBtn2

		Then("The user assignemnt should be updated")
		id("main").webElement.findElements(By.cssSelector("td.action-col")).get(1).getText should equal("Feedback needs publishing")

	}

	"Department admin" should "be able to adjust marks and grades for assignment" in as(P.Admin1) {

		openAssignmentsScreen()
		addMarksGrades()
		changeMarksGrades()
	}
}
