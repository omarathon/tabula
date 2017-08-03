package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

import scala.collection.JavaConverters._

class ReplaceMarkerSingleUseWorkflowTest extends BrowserTest with CourseworkFixtures {

	private def openModifyMarkerScreen(): Unit = {
		When("I go the admin page")
		click on linkText("Test Services")
		Then("I should be able to click on the Marking workflows option")
		val toolbar = findAll(className("dept-toolbar")).next().underlying
		click on toolbar.findElement(By.partialLinkText("Marking workflows"))

		eventually {
			Then("I should reach the marking workflowspage")
			currentUrl should include("/2017/markingworkflows")
		}
	}

	"Department admin" should "be able to modify markers in single use workflows" in as(P.Admin1) {
		openModifyMarkerScreen()
	}
}