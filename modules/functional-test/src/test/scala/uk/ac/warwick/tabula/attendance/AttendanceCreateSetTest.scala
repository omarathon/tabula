package uk.ac.warwick.tabula.attendance

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.FunctionalTestAcademicYear
import org.openqa.selenium.By
import scala.collection.JavaConverters._

class AttendanceCreateSetTest extends AttendanceFixture with GivenWhenThen{

	val year = FunctionalTestAcademicYear.current.startYear

	"A Member of staff" should "be able to create monitoring point sets" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		When("I go to /attendance/manage/xxx/sets/add/2013?createType=blank")
		go to Path("/attendance/manage/xxx/sets/add/2013?createType=blank")

		Then("I can open the routes drop-down")
		click on className("routeAndYearPicker").webElement.findElement(By.className("expand-button"))
		className("routeAndYearPicker").webElement.findElement(By.className("collapsible-target")).isDisplayed should be (true)

		And("I can choose a year of a route")
		click on cssSelector(".scroller table td.year_1 input[type=checkbox]")
		className("routes-count").webElement.getText should be ("is 1 route")

		And("I can add a new point")

		click on cssSelector("a.new-point")

		eventuallyAjax {
			id("modal").webElement.findElement(By.tagName("h2")) should not be (None)
			id("modal").webElement.isDisplayed should be (true)
		}

		click on id("name")
		pressKeys("Point 1")
		singleSel("validFromWeek").value = "1"
		singleSel("requiredFromWeek").value = "2"
		click on cssSelector(".modal-footer button.btn-primary")

		eventuallyAjax {
			id("modal").webElement.isDisplayed should be (false)
			findAll(cssSelector(".monitoring-points .point")).size should be (1)
			findAll(cssSelector("#addMonitoringPointSet input[name='monitoringPoints[0].name']")).size should be (1)
		}


		And("I can add a new point in another term")

		click on cssSelector("a.new-point")

		eventuallyAjax {
			id("modal").webElement.findElement(By.tagName("h2")) should not be (None)
			id("modal").webElement.isDisplayed should be (true)
		}

		click on id("name")
		pressKeys("Point 2")
		singleSel("validFromWeek").value = "15"
		singleSel("requiredFromWeek").value = "15"
		click on cssSelector(".modal-footer button.btn-primary")

		eventuallyAjax {
			id("modal").webElement.isDisplayed should be (false)
			findAll(cssSelector(".monitoring-points .point")).size should be (2)
			findAll(cssSelector("#addMonitoringPointSet input[name='monitoringPoints[0].name']")).size should be (1)
			findAll(cssSelector("#addMonitoringPointSet input[name='monitoringPoints[1].name']")).size should be (1)
		}

		And("I can create the point set")
		click on cssSelector("#addMonitoringPointSet input[type=submit]")

		Then("I am redirected to the manageing home page")
		eventually(currentUrl should include("/attendance/manage/xxx"))

	}

}
