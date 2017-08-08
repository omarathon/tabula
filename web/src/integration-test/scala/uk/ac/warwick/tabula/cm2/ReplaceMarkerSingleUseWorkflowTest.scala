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

		val getCurrentYear = linkText("16/17");
		click on getCurrentYear

		eventuallyAjax {
			Then("I should be on the 16/17 version of the page")
			currentUrl should include("/2016/markingworkflows")
		}

		val addToYear = id("main").webElement.findElement(By.linkText("Add to 17/18"));
		click on addToYear

		eventuallyAjax {
			Then("I should reach the marking workflows page")
			currentUrl should include("/2017/markingworkflows")
		}

		val currentMarker = id("main").webElement.findElements(By.tagName("td")).get(2).getText == "Marker: tabula-functest-marker1 user"
		currentMarker should be (true)

		val modifyBtn = id("main").webElement.findElement(By.cssSelector(" td a.btn-default"))
		click on modifyBtn

		eventuallyAjax {
			Then("I should reach the modify workflows options page")
			currentUrl should include("/edit")
		}

		val replaceLink = id("main").webElement.findElement(By.partialLinkText("Replace marker"))
		click on replaceLink

		eventuallyAjax {
			Then("I should reach the modify workflows page")
			currentUrl should include("/replace")
		}

	}

	private def replaceMarker(): Unit = {

		val markerToReplace = id("oldMarker").webElement
		click on markerToReplace

		val oldMarker = markerToReplace.findElement(By.cssSelector("option[value=tabula-functest-marker1]"))
		oldMarker.isDisplayed should be (true)
		click on oldMarker

		val newMarker = id("main").webElement.findElement(By.cssSelector("input[name=newMarker]"))
		newMarker.sendKeys("tabula-functest-marker2")

		val saveButton = cssSelector("input.btn-primary")
		click on saveButton

		eventuallyAjax {
			Then("I should reach the modify workflows page again")
			currentUrl should include("/xxx/2017/markingworkflows")
		}

		val addedMarker = id("main").webElement.findElements(By.tagName("td")).get(2).getText == "Marker: tabula-functest-marker2 user"
		addedMarker should be (true)

	}

	"Department admin" should "be able to modify markers in single use workflows" in as(P.Admin1) {

		openModifyMarkerScreen()
		replaceMarker()

	}
}