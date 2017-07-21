package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

import scala.collection.JavaConverters._

class CopyAssignmentsTest extends BrowserTest with CourseworkFixtures {

	private def openAssignmentsScreen(): Unit = {
		When("I go the admin page")
		if (!currentUrl.contains("/department/xxx/20")) {
			click on linkText("Test Services")
		}

		Then("I should be able to click on the Assignments dropdown")
		val toolbar = findAll(className("dept-toolbar")).next().underlying
		click on toolbar.findElement(By.partialLinkText("Assignments"))

		And("I should see the create assignments from previous in the assignments menu option")
		val createAssignmentsLink = toolbar.findElement(By.partialLinkText("Create assignments from previous"))
		eventually(timeout(45.seconds), interval(300.millis)) ({
			createAssignmentsLink.isDisplayed should be {
				true
			}
		})

		When("I click the create assignments from previous link")
		click on createAssignmentsLink

		eventually(timeout(45.seconds), interval(300.millis)) ({
			Then("I should reach the create assignments from previous page")
			currentUrl should include("/copy-assignments")
		})
	}

	private def copyCm1Assignment(): Unit = {
		When("I select the Premarked assignment checkbox")
		val tbody = id("main").webElement.findElement(By.tagName("tbody"))
		val row = tbody.findElements(By.tagName("tr")).asScala.find({_.findElements(By.tagName("td")).size > 0}).find({_.findElement(By.xpath("//*[contains(text(),'Premarked assignment') and not(contains(text(), 'Premarked assignment CM2'))]")).isDisplayed})
		row should be (defined)

		val checkbox = row.get.findElement(By.xpath("//input[@class='collection-checkbox']"))
		click on checkbox
		checkbox.isSelected should be (true)

		And("I choose to confirm")
		val confirmBtn = id("main").webElement.findElement(By.xpath("//*[@value='Confirm']"))
		click on confirmBtn

		And("I choose to save the changes")
		val confirmModalBtn = id("main").webElement.findElement(By.xpath("//button[@name='submit']"))
		click on confirmModalBtn

		eventually(timeout(45.seconds), interval(300.millis)) ({

			Then("The changes are saved and I am redirected")
			currentUrl should not include ("copy-assignments")

		})

		When("I expand Test Module 2")
		val testModule = id("main").webElement.findElement(By.className("fa-chevron-right"))
		click on testModule

		eventually(timeout(45.seconds), interval(300.millis)) ({

			And("Assignments should be displayed")
			val assignment1 = id("main").webElement.findElement(By.xpath("//*[contains(text(),'Premarked assignment')]"))
			assignment1.isDisplayed should be(true)

		})

		And("3 assignments should be found")
		var cm1AssignmentsCnt = id("main").webElement.findElements(By.xpath("//*[contains(text(),'Premarked assignment') and not(contains(text(), 'Premarked assignment CM2'))]")).size()
		var cm2AssignmentsCnt = id("main").webElement.findElements(By.xpath("//*[contains(text(),'Premarked assignment CM2')]")).size()

		cm1AssignmentsCnt should be(2)
		cm2AssignmentsCnt should be(1)

	}

	private def copyCm2Assignment(): Unit = {

		When("I select the Premarked assignment checkbox")
		val tbody2 = id("main").webElement.findElement(By.tagName("tbody"))
		val row2 = tbody2.findElements(By.tagName("tr")).get(3)
		row2.getText() should be ("Premarked assignment CM2 16/17")

		val td2 = row2.findElements(By.tagName("td")).get(0)

		val checkbox2 = td2.findElement(By.xpath("input[@class='collection-checkbox']"))

		click on checkbox2
		checkbox2.isSelected should be (true)

		When("I choose to confirm")
		val confirmBtn = id("main").webElement.findElement(By.xpath("//input[@value='Confirm']"))
		click on confirmBtn

		When("I choose to save the changes")
		val confirmModalBtn = id("main").webElement.findElement(By.xpath("//button[@name='submit']"))
		click on confirmModalBtn

		eventually(timeout(45.seconds), interval(300.millis)) ({

			Then("The changes are saved and I am redirected")
			currentUrl should not include ("copy-assignments")

		})

		When("I expand Test Module 2")
		val testModule = id("main").webElement.findElement(By.className("fa-chevron-right"))
		click on testModule

		eventually(timeout(45.seconds), interval(300.millis)) ({

			Then("Assignments should be displayed")
			val content = id("main").webElement.findElement(By.xpath("//*[contains(text(),'Premarked assignment CM2')]"))
			content.isDisplayed should be(true)

		})

		Then("4 assignments should be found")
		var cm1AssignmentsCnt = id("main").webElement.findElements(By.xpath("//*[contains(text(),'Premarked assignment') and not(contains(text(), 'Premarked assignment CM2'))]")).size()
		var cm2AssignmentsCnt = id("main").webElement.findElements(By.xpath("//*[contains(text(),'Premarked assignment CM2')]")).size()

		cm1AssignmentsCnt should be(2)
		cm2AssignmentsCnt should be(2)

	}

	"Department admin" should "be able to copy assignments" in as(P.Admin1) {
		openAssignmentsScreen()
		copyCm1Assignment()
		openAssignmentsScreen()
		copyCm2Assignment()

	}
}