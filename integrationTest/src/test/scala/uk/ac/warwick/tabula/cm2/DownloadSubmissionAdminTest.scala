package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

class DownloadSubmissionAdminTest extends BrowserTest with CourseworkFixtures {

	private def openSummaryScreen(): Unit = {

		When("I go the admin page")
		if (!currentUrl.contains("/department/xxx/20")) {
			click on linkText("Test Services")
		}

		loadCurrentAcademicYearTab()

		val testModule =id("main").webElement.findElements(By.cssSelector(".fa-chevron-right")).get(0)
		click on testModule

		eventuallyAjax {
			val cm2Assignment =id("main").webElement.findElements(By.cssSelector("h5.assignment-name a")).get(0)
			click on cm2Assignment
		}

		eventuallyAjax {
			currentUrl.contains("/summary") should be(true)
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

		click on downloadDropdown

		Then("The link to download as pdf should be enabled")
		val menuOption = id("main").webElement.findElements(By.cssSelector(".dropdown-menu li a.download-pdf")).get(0)
		disabledlinks.getAttribute("class").contains("disabled") should be (false)

		And("The link should point to the pdf file")
		menuOption.getAttribute("href") should include("/submissions.pdf")
		click on menuOption

	}

	private def downloadAll(): Unit = {

			When("I click on the Download dropdown without selecting any students")
			val downloadDropdown = id("main").webElement.findElements(By.cssSelector("button.dropdown-toggle")).get(0)
			click on downloadDropdown

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

			click on downloadDropdown

			And("The link to download as zip should be enabled")
			val menuOption = id("main").webElement.findElements(By.cssSelector(".dropdown-menu li a.form-post")).get(0)
			disabledlinks.getAttribute("class").contains("disabled") should be (false)


			And("The link should point to the zip file")
			menuOption.isDisplayed should be(true)
			menuOption.getAttribute("href") should include("/submissions.zip")
			click on menuOption

	}
	private def deleteSubmission(): Unit = {

		When("I click on the Download dropdown without selecting any students")
		val downloadDropdown = id("main").webElement.findElements(By.cssSelector("button.dropdown-toggle")).get(0)
		click on downloadDropdown

		Then("The link to download as zip should be disabled")
		val disabledlinks = id("main").webElement.findElements(By.cssSelector("ul.dropdown-menu li")).get(0)
		disabledlinks.getAttribute("class").contains("disabled") should be (true)

		When("I select a student")
		val student1Checkbox = id("main").webElement.findElements(By.cssSelector(".collection-checkbox")).get(0)
		click on student1Checkbox

		click on downloadDropdown

		And("The link to delete submissions should be enabled")
		val menuOption = id("main").webElement.findElements(By.cssSelector(".dropdown-menu li a.form-post")).get(1)
		disabledlinks.getAttribute("class").contains("disabled") should be (false)


		And("The link should point to the zip file")
		menuOption.isDisplayed should be(true)
		menuOption.getAttribute("href") should include("/delete")
		click on menuOption

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

	"Admin" should "be able to download submissions" in as(P.Admin1) {

		openSummaryScreen()
		downloadAll()
		uncheckStudents()
		downloadAsPdf()
		uncheckStudents()
		deleteSubmission()
	}
}