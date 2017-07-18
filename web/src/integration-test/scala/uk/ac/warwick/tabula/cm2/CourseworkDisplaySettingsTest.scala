package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

class CourseworkDisplaySettingsTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	"Department admin" should "be able to set display settings for a department" in as(P.Admin1) {
		openDisplaySettings()

		When("the administrator selects studentname checkbox")
		checkbox("showStudentName").select()
		And("submits the form")
		cssSelector("#displaySettingsCommand input.btn-primary").webElement.click()

		Then("We are redirected to admin department home page")
		eventually {
			// Ensure that we've been redirected back to the dept admin page
			currentUrl should endWith("/department/xxx")

		}

		// Check that when we go back to the page, all of the settings have been populated
		openDisplaySettings()
		And("showStudentName is selected")
		checkbox("showStudentName").isSelected should be {
			true
		}
	}

	"Department admin" should "be able to set default group signup method for a department" in {

		Given("The smallGroupTeachingStudentSignUp feature is enabled")
		enableFeature("smallGroupTeachingStudentSignUp")

		as(P.Admin1) {
			openDisplaySettings()

			And("the administrator can see the option to set a default signup method")
			radioButtonGroup("defaultGroupAllocationMethod") should not be null


			And("the administrator selects 'Manual Allocation' and submits")
			radioButtonGroup("defaultGroupAllocationMethod").value = "Manual"
			submit()

			openDisplaySettings()
			And("The settings page should show 'Manual Allocation' as the selected method")
			radioButtonGroup("defaultGroupAllocationMethod").selection should be(Some("Manual"))

			And("the administrator selects 'Student Sign-up' and submits")
			radioButtonGroup("defaultGroupAllocationMethod").value = "StudentSignUp"
			submit()

			openDisplaySettings()
			And("The settings page defaultGroupAllocationMethod option should be StudentSignUp")
			radioButtonGroup("defaultGroupAllocationMethod").selection should be(Some("StudentSignUp"))


		}
	}

	private def openDisplaySettings(): Unit = {
		When("I go the admin page")
		if (!currentUrl.contains("/department/xxx")) {
			click on linkText("Test Services")
		}

		Then("I should be able to click on the Setting dropdown")
		val toolbar = findAll(className("dept-toolbar")).next().underlying
		click on toolbar.findElement(By.partialLinkText("Settings"))

		And("I should see the department settings menu option")
		val departmentLink = toolbar.findElement(By.partialLinkText("Department settings"))
		eventually {
			departmentLink.isDisplayed should be {
				true
			}
		}

		When("I click the department setting link")
		click on departmentLink

		Then("I should reach the department settings page")
		currentUrl should include("/display")

	}
}