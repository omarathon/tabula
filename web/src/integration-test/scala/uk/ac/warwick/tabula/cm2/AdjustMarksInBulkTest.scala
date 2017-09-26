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
				And("ensure I'm looking at this year")
				loadCurrentAcademicYearTab()

				currentUrl.contains("/department/xxx") should be(true)
			}

			eventually {
				val testModule = id("main").webElement.findElements(By.cssSelector(".fa-chevron-right")).get(0)
				click on testModule
			}

			eventuallyAjax {
				val cm2Assignment = id("main").webElement.findElements(By.cssSelector("h5.assignment-name a")).get(0)
				click on cm2Assignment
			}

			eventuallyAjax {
				currentUrl.contains("/summary") should be(true)
			}

			When("I click on the Feedback dropdown without selecting any students")
			val feedbackDropdown = id("main").webElement.findElements(By.cssSelector("a.dropdown-toggle")).get(5)
			click on feedbackDropdown

			val adjustmentsLinkHolder = id("main").webElement.findElements(By.cssSelector("li")).get(27)
			adjustmentsLinkHolder.getAttribute("class")contains("disabled") should be(true)

			val selectAll = id("main").webElement.findElements(By.cssSelector(".collection-check-all")).get(0)
			click on selectAll

			click on feedbackDropdown

			adjustmentsLinkHolder.getAttribute("class") contains ("disabled") should be(false)
			val adjustmentsLink = id("main").webElement.findElements(By.cssSelector(".dropdown-menu li .form-post")).get(4)
			adjustmentsLink.isDisplayed should be (true)
			click on adjustmentsLink

			eventually {
				currentUrl.contains("/feedback/adjustments") should be(true)
			}

			val bulkAdjustButton = id("main").webElement.findElements(By.cssSelector("div.pull-right a")).get(0)
			bulkAdjustButton.isEnabled should be (true)
			click on bulkAdjustButton

	}

	private def adjustMarks(): Unit = {

	}

	private def uncheckStudents(): Unit = {

		val student1Checkbox = id("main").webElement.findElements(By.cssSelector(".collection-checkbox")).get(0)
		if(student1Checkbox.isSelected){
			click on student1Checkbox
		}
		val student2Checkbox = id("main").webElement.findElements(By.cssSelector(".collection-checkbox")).get(1)
		if(student2Checkbox.isSelected){
			click on student2Checkbox
		}

	}

	"Admin" should "be able to ajust marks in bulk" in as(P.Admin1) {

		openBulkAdjustmentScreen()
		adjustMarks()
		//uncheckStudents()

	}
}