package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

import scala.collection.JavaConverters._

class DeleteAssignmentTest extends BrowserTest with CourseworkFixtures {

	private def openAssignmentsScreen(): Unit = {

		When("I go the admin page")
		if (!currentUrl.contains("/department/xxx/20")) {
			click on linkText("Test Services")
		}

		// make sure we are looking at the latest academic year
		val yearNav = findAll(cssSelector(".navbar-tertiary .navbar-nav")).next().underlying
		val latestAcademicYear = yearNav.findElements(By.cssSelector("li")).asScala
		click on latestAcademicYear.last

		When("I expand module XXX02")
		val moduleBlock = id("main").webElement.findElements(By.cssSelector("h4.with-button")).get(1)
		val arrow = moduleBlock.findElement(By.cssSelector(".fa-chevron-right"))
		click on arrow
		
		Then("The  module should expand")
		eventually(timeout(45.seconds), interval(300.millis)) ({
			And("I should find an assignment with no submissions")
			val premarkedAssignmentsSize = id("main").webElement.findElements(By.xpath("//*[contains(text(),'Premarked assignment CM2')]")).size()
			premarkedAssignmentsSize should be(1)
		})
	}

	private def checkUndeletableAssignment(): Unit = {

		val editBtn = id("main").webElement.findElements(By.cssSelector("a.btn-xs")).get(4)
		click on editBtn

		eventually {
			Then("I should reach the edit page")
			currentUrl should include("/edit")
		}

		cssSelector("p.alert-info").webElement.getText should include ("It's not possible to delete this assignment because it has submissions and/or feedback is published.")

	}

	private def deleteAssignment(): Unit = {

		val editBtn = id("main").webElement.findElements(By.cssSelector("a.btn-xs")).get(5)
		click on editBtn

		eventually {
			Then("I should reach the edit page")
			currentUrl should include("/edit")
		}

		When("I click on the delete button")
		cssSelector(".btn-danger").webElement.getText should be ("delete")
		click on cssSelector(".btn-danger")

		When("Then I should go to the confirmation page")
		eventually {
			Then("I should reach the delete page")
			currentUrl should include("/delete")
		}

		And("The delete button should be disabled")
		cssSelector(".btn-danger").webElement.isEnabled should be (false)

		When("When I click on the confirmation checkbox")
		click on id("confirmCheck").webElement
		eventually {
			Then("The delete button should be enabled")
			cssSelector(".btn-danger").webElement.isEnabled should be(true)
			click on cssSelector(".btn-danger")
		}
		eventuallyAjax {
			Then("I should reach the delete page")
			currentUrl should not include("/edit")
		}

		Then("I should find not an assignment with no assignments")
		val noSubmissionsAssignmentsSize = id("main").webElement.findElements(By.xpath("//*[contains(text(),'No Submissions Assignment CM2')]")).size()
		noSubmissionsAssignmentsSize should be (0)

	}

	"Department admin" should "be able to delete an assignment" in {
		withAssignment("xxx02", "No Submissions Assignment CM2"){ assignmentId => }

			openAssignmentsScreen()
			deleteAssignment()
			openAssignmentsScreen()
			checkUndeletableAssignment()
	}
}