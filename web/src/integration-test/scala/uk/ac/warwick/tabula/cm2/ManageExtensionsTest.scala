package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

class ManageExtensionsTest extends BrowserTest with CourseworkFixtures {

	val ExtensionGuidelines = "You must be dying of something awful"
	val ExtensionInfoUrl = "http://warwick.ac.uk/"


	"Department admin" should "be able to enable extensions and save other extension settings" in as(P.Admin1) {
		openExtensionRequests()


	}
	private def openExtensionRequests(): Unit = {
		When("I go the admin page")
		if (!currentUrl.contains("/department/xxx")) {
			click on linkText("Test Services")
		}

		Then("I should be able to click on the Extensions Requests")
		val toolbar = findAll(className("dept-toolbar")).next().underlying
		click on toolbar.findElement(By.partialLinkText("Extension requests"))

		And("I should see the extension requests page")
		val extensionsLink = toolbar.findElement(By.partialLinkText("Extension settings"))
		eventually {
			currentUrl should include("/extensions")
		}

		}
}