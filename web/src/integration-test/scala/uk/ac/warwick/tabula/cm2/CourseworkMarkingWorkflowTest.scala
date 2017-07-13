package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

import scala.collection.JavaConverters._

class CourseworkMarkingWorkflowTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

/**	private def openMarkingWorkflowSettings(): Unit = {
		When("I click on the Marking workflows link")
		click on linkText("Marking workflows")

		Then("I should reach the marking workflow page")
		currentUrl should include("/markingworkflows")

	}   **/

	private def loadCreateMarkingworkflow(): Unit = {
		When("I click create")
		click on partialLinkText("Create")
		Then("I should reach the create marking workflow page")
		currentUrl should include("/add")

	}

	private def createSingleMarkingWorkflow(workflowName: String): Unit = {
		loadCreateMarkingworkflow()
		And("I enter the necessary data")
		textField("workflowName").value = workflowName
		singleSel("workflowType").value = "Single"
		textField("markersA").value = P.Marker1.usercode



		And("I click  add another to create one more marker")
		click on className("markersA").webElement.findElement(By.cssSelector("button.btn"))
		eventually {
			findAll(cssSelector(".markersA input.flexi-picker")).toList.count(_.isDisplayed) should be (2)
		}

		And("I enter another marker")
		new TextField(findAll(cssSelector("input.flexi-picker")).toList.apply(1).underlying).value = P.Marker2.usercode
		And("I submit the form")
		submit()

	}

	private def createDoubleMarkingWorkflow(workflowName: String): Unit = {
		loadCreateMarkingworkflow()
		And("I enter the necessary data")
		textField("workflowName").value = workflowName
		singleSel("workflowType").value = "Double"
		textField("markersA").value = P.Marker1.usercode
		textField("markersB").value = P.Marker2.usercode


		And("I click  add another to create one more marker for marker list B")
		click on className("markersB").webElement.findElement(By.cssSelector("button.btn"))
		eventually {
			findAll(cssSelector(".markersB input.flexi-picker")).toList.count(_.isDisplayed) should be (2)
		}

		And("I enter another marker")
		new TextField(findAll(cssSelector(".markersB input.flexi-picker")).toList.apply(1).underlying).value = P.Marker3.usercode
		And("I submit the form")
		submit()

	}

	private def deleteMarkingWorkflow(workflowName: String):Unit = {
		When("I delete a workflow")
		({
			val tbody = className("table").webElement.findElement(By.tagName("tbody"))
			val row = tbody.findElements(By.tagName("tr")).asScala.find({
				_.findElement(By.tagName("td")).getText == workflowName
			})
			row should be('defined)

			click on row.get.findElement(By.partialLinkText("Delete"))
		})

		Then("I should reach the delete marking workflow page")
		eventually {
			currentUrl should endWith ("/delete")
		}
		And("I  click delete button")
		click on cssSelector("input[value=Delete]")
}

	private def listMarkingWorkflow(workflowsize: Int): Unit = {
		When("I view departmental markingworkflows")
		currentUrl should include("/markingworkflows")

		Then("I see markingworkflow record")
		cssSelector("table tbody tr").findAllElements.size should be (workflowsize)
	}

	private def editMarkingWorkflow(): Unit = {
			// brackets/braces isolate vals and prevent block being treated as implicit for When() above
			When("I edit Single marker workflow 2")
		({
			val tbody = className("table-sortable").webElement.findElement(By.tagName("tbody"))
			val row = tbody.findElements(By.tagName("tr")).asScala.find({
				_.findElement(By.tagName("td")).getText == "Single marker workflow 2"
			})
			row should be('defined)
			click on row.get.findElement(By.partialLinkText("Modify"))

			eventually {
				currentUrl should endWith ("/edit")
			}

		})

		Then("I should see the correct fields")
		textField("workflowName").value should be ("Single marker workflow 2")
		textField("markersA").value should be (P.Marker1.usercode)

		When("I cancel that edit")
		click on linkText("Cancel")
		Then("I should be redirected to workflow list")
		eventually {
			currentUrl should endWith ("/markingworkflows")
		}
	}


	"Department admin" should "be able to manage(create/edit/delete) marking workflows" in as(P.Admin1) {
		openAdminPage()
		openMarkingWorkflowSettings()

		//There should be existing single marking one at first created via setup
		listMarkingWorkflow(1)

		createSingleMarkingWorkflow("Single marker workflow 2")

		Then("I should be redirected back to the marking workflow page")
		currentUrl should endWith ("/markingworkflows")

		listMarkingWorkflow(2)
		editMarkingWorkflow()

		//create another
		createDoubleMarkingWorkflow("Double marker workflow 3")
		Then("I should be redirected back to the marking workflow page")
		currentUrl should endWith ("/markingworkflows")
		listMarkingWorkflow(3)

		deleteMarkingWorkflow("Single marker workflow 2")
		eventually {
			currentUrl should include ("/markingworkflows")
		}
		And("It should have removed the workflow")
		listMarkingWorkflow(2)
		And("No row with that workflow should be there")
		({
			val tbody = className("table").webElement.findElement(By.tagName("tbody"))
			val row = tbody.findElements(By.tagName("tr")).asScala.find({
				_.findElement(By.tagName("td")).getText == "Single marker workflow 2"
			})
			row should not be('defined)
		})
	}
}