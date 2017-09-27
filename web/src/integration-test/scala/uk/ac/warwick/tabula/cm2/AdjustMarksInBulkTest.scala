package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

class AdjustMarksInBulkTest extends BrowserTest with CourseworkFixtures {

	private def openBulkAdjustmentScreen(): Unit = {

			click on linkText("Coursework Management")
			currentUrl.contains("/coursework/") should be(true)

			When("I go the admin page")
			click on linkText("Test Services")

			eventually {
				And("ensure I'm looking at the current year")
				loadCurrentAcademicYearTab()

				Then("I should get to the department page")
				currentUrl.contains("/department/xxx") should be(true)
			}

			eventually {
				When("I expand the the module")
				val testModule = id("main").webElement.findElements(By.cssSelector(".fa-chevron-right")).get(0)
				click on testModule
			}

			eventuallyAjax {
				And("click on the cm2 assignment")
				val cm2Assignment = id("main").webElement.findElements(By.cssSelector("h5.assignment-name a")).get(0)
				click on cm2Assignment
			}

			eventuallyAjax {
				Then("I should get to the summary page")
				currentUrl.contains("/summary") should be(true)
			}

			When("I click on the Feedback dropdown without selecting any students")
			val feedbackDropdown = id("main").webElement.findElements(By.cssSelector("a.dropdown-toggle")).get(5)
			click on feedbackDropdown

			Then("The adjustments link is disabled")
			val adjustmentsLinkHolder = id("main").webElement.findElements(By.cssSelector("li")).get(27)
			adjustmentsLinkHolder.getAttribute("class")contains("disabled") should be(true)

			When("I select the all student checkbox")
			val selectAll = id("main").webElement.findElements(By.cssSelector(".collection-check-all")).get(0)
			click on selectAll

			And("Select the feeback dropdown")
			click on feedbackDropdown

			Then("The adjustments link is enabled")
			adjustmentsLinkHolder.getAttribute("class") contains ("disabled") should be(false)
			val adjustmentsLink = id("main").webElement.findElements(By.cssSelector(".dropdown-menu li .form-post")).get(4)
			adjustmentsLink.isDisplayed should be (true)

			When("I click on the adjustments link")
			click on adjustmentsLink

			Then("I am taken to the adjustments page")
			eventually {
				currentUrl.contains("/feedback/adjustments") should be(true)
			}

			And("I click on the bulk adjust buttons")
			val bulkAdjustButton = id("main").webElement.findElements(By.cssSelector("div.pull-right a")).get(0)
			bulkAdjustButton.isEnabled should be (true)
			click on bulkAdjustButton

	}

	private def adjustMarks(): Unit = {

		When("I upload an adjustments file")
		ifPhantomJSDriver(
			operation = { d =>
				// This hangs forever for some reason in PhantomJS if you use the normal pressKeys method
				d.executePhantomJS("var page = this; page.uploadFile('input[type=file]', '" + getClass.getResource("/adjustments.xlsx").getFile + "');")
			},
			otherwise = { _ =>
				click on find(cssSelector("input[type=file]")).get
				pressKeys(getClass.getResource("/adjustments.xlsx").getFile)
			}
		)

		And("Click on the upload button")
		click on cssSelector("button.btn-primary").webElement
		Then("summary confirmations should be displayed")
		cssSelector("input#privateAdjustment1").webElement.isEnabled should be (true)

		When("I click on the confirm button")
		click on cssSelector(".btn-primary").webElement

		Then("I am returned to the summary page")
		currentUrl.contains("/summary") should be(true)

	}

	"Admin" should "be able to ajust marks in bulk" in as(P.Admin1) {

		openBulkAdjustmentScreen()
		adjustMarks()

	}
}