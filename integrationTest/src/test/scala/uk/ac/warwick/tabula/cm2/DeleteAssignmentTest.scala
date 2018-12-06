package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.{AcademicYear, BrowserTest}

class DeleteAssignmentTest extends BrowserTest with CourseworkFixtures {

	private def openAssignmentsScreen(title: String): Unit = {

		When("I go the admin page")
		if (!currentUrl.contains("/department/xxx/20")) {
			click on linkText("Test Services")
		}

		// make sure we are looking at the current academic year
		click on cssSelector(".navbar-tertiary").webElement.findElement(By.partialLinkText(s"${AcademicYear.now().toString}"))

		When("I expand module XXX02")
		val moduleBlock = id("main").webElement.findElements(By.cssSelector("h4.with-button")).get(1)
		val arrow = moduleBlock.findElement(By.cssSelector(".fa-chevron-right"))
		click on arrow
		
		Then("The  module should expand")
		eventually(timeout(45.seconds), interval(300.millis)) ({
			And("I should find an assignment with no submissions")
			val premarkedAssignmentsSize = id("main").webElement.findElements(By.xpath("//*[contains(text(),'" ++ title ++ "')]")).size()
			premarkedAssignmentsSize should be(1)
		})
	}

	private def checkUndeletableAssignment(title: String): Unit = {
		val path = "//*[contains(text(),'" ++ title ++ "')]"
		eventually {
			val assignmentRow = id("main").webElement.findElements(By.xpath(path))
			click on assignmentRow.get(0)
		}
		eventually(currentUrl should endWith("/summary"))
		When("I click on the edit button")
		click on partialLinkText("Edit assignment")

		eventually {
			Then("I should reach the edit page")
			currentUrl should include("/edit")
		}

		cssSelector("p.alert-info").webElement.getText should include ("It's not possible to delete this assignment because it has submissions and/or feedback is published.")
	}

	private def deleteAssignment(title: String): Unit = {
		val path = "//*[contains(text(),'" ++ title ++ "')]"

		eventually {
			val assignmentRow = id("main").webElement.findElements(By.xpath(path))
			click on assignmentRow.get(0)
		}
		eventually(currentUrl should endWith("/summary"))
		When("I click on the edit button")
		click on partialLinkText("Edit assignment")

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
		eventually {
			Then("I should reach the delete page")
			currentUrl should not include("/edit")
		}

		Then("I should find not an assignment with no assignments")
		val noSubmissionsAssignmentsSize = id("main").webElement.findElements(By.xpath(path)).size()
		noSubmissionsAssignmentsSize should be (0)

	}

	"Department admin" should "be able to delete an assignment" in {
		val assignmentTitle = "No Submissions Assignment CM2"
		val assignmentTitle1 = "Premarked assignment CM2"
		withAssignment("xxx02", assignmentTitle){ assignmentId => }

			openAssignmentsScreen(assignmentTitle)
			deleteAssignment(assignmentTitle)
			openAssignmentsScreen(assignmentTitle1)
			checkUndeletableAssignment(assignmentTitle1)
	}
}