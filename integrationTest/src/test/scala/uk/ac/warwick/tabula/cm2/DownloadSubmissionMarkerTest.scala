package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

class DownloadSubmissionMarkerTest extends BrowserTest with CourseworkFixtures {

	private def openMarkingScreen(): Unit = {

		When("I select the relevant button on the CM2 assignment")
		//ensure page has loaded table elements otherwise wait.
		eventuallyAjax {
			val testModulerow = id("main").webElement.findElements(By.cssSelector("span.mod-code")).get(0)
			click on testModulerow
		}

		eventuallyAjax {

			val reviewAssignmentBtn = id("main").webElement.findElements(By.cssSelector(".btn-block")).get(1)
			reviewAssignmentBtn.getText should include("Review")
			click on reviewAssignmentBtn
		}
		Then("I should be taken to the Marking page")

		eventuallyAjax {
				currentUrl should include("/marker/tabula-functest-marker1")
		}

	}

	private def downloadAsPdf(): Unit = {

		When("I click on the Download dropdown without selecting any students")
		val downloadDropdown = id("main").webElement.findElements(By.cssSelector("button.dropdown-toggle")).get(0)
		click on downloadDropdown

		Then("The link to download as pdf should be disabled")
		val disabledlinks = id("main").webElement.findElements(By.cssSelector("ul.dropdown-menu li")).get(0)
		disabledlinks.getAttribute("class").contains("disabled") should be (true)

		When("I select a student")
		val student1Checkbox = id("main").webElement.findElements(By.cssSelector(".collection-checkbox")).get(0)
		click on student1Checkbox

		eventually {

			Then("The link to download as pdf should be enabled")
			val menuOption = id("main").webElement.findElements(By.cssSelector(".dropdown-menu li a.download-pdf")).get(0)
			disabledlinks.getAttribute("class").contains("disabled") should be (false)

			And("The link should point to the pdf file")
			menuOption.getAttribute("href") should include("/marker/tabula-functest-marker1/submissions.pdf")
			click on menuOption
		}

	}

	private def downloadAll(): Unit = {

			When("I click on the Download dropdown without selecting any students")
			eventuallyAjax {
				val downloadDropdown = id("main").webElement.findElements(By.cssSelector("button.dropdown-toggle")).get(0)
				click on downloadDropdown
			}
			Then("The link to download as zip should be disabled")
		  val disabledlinks = id("main").webElement.findElements(By.cssSelector("ul.dropdown-menu li")).get(0)
		  disabledlinks.getAttribute("class").contains("disabled") should be (true)

			When("I select a student")
			val student1Checkbox = id("main").webElement.findElements(By.cssSelector(".collection-checkbox")).get(0)
			click on student1Checkbox

			And("I select a second student")
			val student2Checkbox = id("main").webElement.findElements(By.cssSelector(".collection-checkbox")).get(1)
			click on student2Checkbox

			Then("The all checkbox should be checked too")
			val allCheckbox = id("main").webElement.findElements(By.cssSelector(".collection-check-all")).get(0)
			allCheckbox.isSelected should be (true)

			eventually {

				And("The link to download as zip should be enabled")
				val menuOption = id("main").webElement.findElements(By.cssSelector(".dropdown-menu li a.form-post")).get(0)
				disabledlinks.getAttribute("class").contains("disabled") should be (false)

				And("The link should point to the zip file")
				menuOption.getAttribute("href") should include("/marker/tabula-functest-marker1/submissions.zip")
				click on menuOption
			}

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

	"First marker" should "be able to download submissions" in as(P.Marker1) {

		click on linkText("Coursework Management")
		currentUrl.contains("/coursework/") should be(true)

		openMarkingScreen()
		downloadAll()
		uncheckStudents()
		downloadAsPdf()

	}
}