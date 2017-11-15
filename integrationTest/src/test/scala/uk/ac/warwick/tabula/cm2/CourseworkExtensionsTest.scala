package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

class CourseworkExtensionsTest extends BrowserTest with CourseworkFixtures {

	val ExtensionGuidelines = "You must be dying of something awful"
	val ExtensionInfoUrl = "http://warwick.ac.uk/"

	private def openExtensionsScreen(): Unit = {
		When("I go the admin page")
		if (!currentUrl.contains("/department/xxx")) {
			click on linkText("Test Services")
		}

		Then("I should be able to click on the Setting dropdown")
		val toolbar = findAll(className("dept-toolbar")).next().underlying
		click on toolbar.findElement(By.partialLinkText("Settings"))

		And("I should see the extension settings menu option")
		val extensionsLink = toolbar.findElement(By.partialLinkText("Extension settings"))
		eventually {
			extensionsLink.isDisplayed should be {
				true
			}
		}

		When("I click the extensions link")
		click on extensionsLink

		Then("I should reach the extension settings page")
		currentUrl should include("/extensions")

		And("I should see allow students to request extensions checkbox")
		checkbox("allowExtensionRequests").isDisplayed should be(true)

	}

	private def enableRequestExtension(): Unit = {
		When("I click request extension button")
		checkbox("allowExtensionRequests").select()

		Then("I should be able to see other fields")
		eventually {
			textArea("extensionGuidelineSummary").isDisplayed should be(true)
			textField("extensionGuidelineLink").isDisplayed should be(true)
			textField("extensionManagers").isDisplayed should be(true)
		}

	}

	private def saveExtensionSettings(): Unit = {
		When("I change various form field values and submit")
		textArea("extensionGuidelineSummary").value = ExtensionGuidelines
		textField("extensionGuidelineLink").value = ExtensionInfoUrl
		textField("extensionManagers").value = P.ExtensionManager1.usercode
		submit()

		Then("I should be redirected back to the dept admin page")
		currentUrl should endWith("/department/xxx")

		//re-open extension screen to cross check changes
		openExtensionsScreen()
		checkbox("allowExtensionRequests").isSelected should be(true)
		textArea("extensionGuidelineSummary").isDisplayed should be(true)
		textArea("extensionGuidelineSummary").value should be(ExtensionGuidelines)
		textField("extensionGuidelineLink").value should be(ExtensionInfoUrl)
		textField("extensionManagers").value should be(P.ExtensionManager1.usercode)
	}


	"Department admin" should "be able to enable extensions and save other extension settings" in as(P.Admin1) {
		openExtensionsScreen()
		enableRequestExtension()
		saveExtensionSettings()

	}
}