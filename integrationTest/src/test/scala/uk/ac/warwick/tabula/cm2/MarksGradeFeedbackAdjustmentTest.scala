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
			eventually(linkText("Fully featured assignment").webElement.isDisplayed should be(true))

			eventually(click on linkText("Fully featured assignment"))
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
		val markPercentage = id("main").webElement.findElement(By.name("marks[0].actualMark"))
		markPercentage.sendKeys("68")

		val grade = id("main").webElement.findElement(By.name("marks[0].actualGrade"))
		grade.sendKeys("2.2")

		val feedbackComment = id("main").webElement.findElement(By.name("marks[0].feedbackComment"))
		feedbackComment.sendKeys("Interesting essay")

		And("I save them")
		val saveBtn = id("main").webElement.findElements(By.cssSelector(".btn-primary")).get(1)
		click on saveBtn

		Then("The marks, grades and feedback should be received")
		id("main").webElement.findElement(By.cssSelector("form#command p")).getText should equal("You are submitting marks for 1 students.")

		val confirmBtn = id("main").webElement.findElements(By.cssSelector(".btn-primary")).get(0)
		click on confirmBtn

		Then("The user assignemnt should be updated")
		eventually(id("main").webElement.findElements(By.cssSelector("td.action-col")).get(1).getText should equal("Feedback needs publishing"))

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
		val markPercentage2 = id("main").webElement.findElement(By.name("marks[0].actualMark"))
		markPercentage2.clear()
		markPercentage2.sendKeys("71")

		val grade2 = id("main").webElement.findElement(By.name("marks[0].actualGrade"))
		grade2.clear()
		grade2.sendKeys("2.1")

		val feedbackComment2 = id("main").webElement.findElement(By.name("marks[0].feedbackComment"))
		feedbackComment2.clear()
		feedbackComment2.sendKeys("Very good")

		And("I save them")
		val saveBtn2 = id("main").webElement.findElements(By.cssSelector(".btn-primary")).get(1)
		click on saveBtn2

		Then("The marks, grades and feedback should be received")
		id("main").webElement.findElement(By.cssSelector("form#command p")).getText should equal("You are submitting marks for 1 students.")

		And("The user should be warned this will replace the existing marks,grades and feedback")
		id("main").webElement.findElement(By.cssSelector("span.warning")).getText should equal("Feedback and/or marks have already been uploaded for this student. These will be overwritten when you click confirm.")

		val confirmBtn2 = id("main").webElement.findElements(By.cssSelector(".btn-primary")).get(0)
		click on confirmBtn2

		Then("The user assignment should be updated")
		id("main").webElement.findElements(By.cssSelector("td.action-col")).get(1).getText should equal("Feedback needs publishing")

	}

	"Department admin" should "be able to adjust marks and grades for assignment" in as(P.Admin1) {

		openAssignmentsScreen()
		addMarksGrades()
		changeMarksGrades()
	}
}
