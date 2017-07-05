package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.data.model.WorkflowCategory
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.{DoubleMarking, SingleMarking, ModeratedMarking}

class SingleUseWorkflowTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	"Department admin" should "be able to create a single use single marker workflow" in {

		withAssignment("xxx01", "Single marker single use") { id =>

			When("I click on the edit button")
			click on partialLinkText("Edit assignment")
			Then("I see the edit details screen")
			eventually(pageSource contains "Edit assignment details" should be {true})

			When("I choose Single use")
			singleSel("workflowCategory").value = WorkflowCategory.SingleUse.code
			Then("I see the options for single use workflows")
			eventuallyAjax(pageSource contains "Marking workflow type" should be {true})

			When("I select single marking add a markers usercode")
			singleSel("workflowType").value = SingleMarking.name
			textField("markersA").value = P.Marker1.usercode

			And("I click 'add another' to create one more marker")
			click on className("markersA").webElement.findElement(By.cssSelector("button.btn"))
			eventually {
				findAll(cssSelector(".markersA input.flexi-picker")).toList.count(_.isDisplayed) should be (2)
			}
			And("I enter another markers usercode")
			new TextField(findAll(cssSelector("input.flexi-picker")).toList.apply(1).underlying).value = P.Marker2.usercode
			And("I submit the form")
			cssSelector(s"input[name=editAndEditDetails]").webElement.click()

			Then("I should be back on the summary page")
			withClue(pageSource) {
				currentUrl should endWith("/summary")
			}

			When("I click on the edit button again")
			click on partialLinkText("Edit assignment")
			Then("I see the edit details screen again with all the workflow information saved")
			eventually(pageSource contains "Edit assignment details" should be {true})
			singleSel("workflowCategory").value should be(WorkflowCategory.SingleUse.code)
			singleSel("workflowType").value should be(SingleMarking.name)
			new TextField(findAll(cssSelector("input.flexi-picker")).toList.head.underlying).value should be (P.Marker1.usercode)
			new TextField(findAll(cssSelector("input.flexi-picker")).toList.apply(1).underlying).value should be (P.Marker2.usercode)

		}
	}

	"Department admin" should "be able to create a single use double seen marking workflow" in {

		withAssignment("xxx01", "Double marker single use") { id =>

			When("I click on the edit button")
			click on partialLinkText("Edit assignment")
			Then("I see the edit details screen")
			eventually(pageSource contains "Edit assignment details" should be {true})

			When("I choose Single use")
			singleSel("workflowCategory").value = WorkflowCategory.SingleUse.code
			Then("I see the options for single use workflows")
			eventuallyAjax(pageSource contains "Marking workflow type" should be {true})

			When("I choose double seen marking")
			singleSel("workflowType").value = DoubleMarking.name
			Then("I see the options double seen marking (2 marker fields)")
			eventuallyAjax(pageSource contains "Add marker" should be {true})
			eventuallyAjax(pageSource contains "Add second marker" should be {true})

			When("I enter markers details")
			textField("markersA").value = P.Marker1.usercode
			click on className("markersA").webElement.findElement(By.cssSelector("button.btn"))
			eventually {
				findAll(cssSelector(".markersA input.flexi-picker")).toList.count(_.isDisplayed) should be (2)
			}
			new TextField(findAll(cssSelector(".markersA input.flexi-picker")).toList.apply(1).underlying).value = P.Marker2.usercode

			textField("markersB").value = P.Marker3.usercode
			click on className("markersB").webElement.findElement(By.cssSelector("button.btn"))

			And("I submit the form")
			cssSelector(s"input[name=editAndEditDetails]").webElement.click()
			Then("I should be back on the summary page")
			withClue(pageSource) {
				currentUrl should endWith("/summary")
			}

			When("I click on the edit button again")
			click on partialLinkText("Edit assignment")
			Then("I see the edit details screen again with all the workflow information saved")
			eventually(pageSource contains "Edit assignment details" should be {true})
			singleSel("workflowCategory").value should be(WorkflowCategory.SingleUse.code)
			singleSel("workflowType").value should be(DoubleMarking.name)
			new TextField(findAll(cssSelector(".markersA input.flexi-picker")).toList.head.underlying).value should be (P.Marker1.usercode)
			new TextField(findAll(cssSelector(".markersA input.flexi-picker")).toList.apply(1).underlying).value should be (P.Marker2.usercode)
			new TextField(findAll(cssSelector(".markersB input.flexi-picker")).toList.head.underlying).value should be (P.Marker3.usercode)
		}
	}

	"Department admin" should "be able to create a single use moderated marking workflow" in {

		withAssignment("xxx01", "Moderated marker single use") { id =>

			When("I click on the edit button")
			click on partialLinkText("Edit assignment")
			Then("I see the edit details screen")
			eventually(pageSource contains "Edit assignment details" should be {true})

			When("I choose Single use")
			singleSel("workflowCategory").value = WorkflowCategory.SingleUse.code
			Then("I see the options for single use workflows")
			eventuallyAjax(pageSource contains "Marking workflow type" should be {true})

			When("I choose double seen marking")
			singleSel("workflowType").value = ModeratedMarking.name
			Then("I see the options double seen marking (2 marker fields)")
			eventuallyAjax(pageSource contains "Add marker" should be {true})
			eventuallyAjax(pageSource contains "Add moderator" should be {true})

			When("I enter markers details")
			textField("markersA").value = P.Marker1.usercode
			click on className("markersA").webElement.findElement(By.cssSelector("button.btn"))
			eventually {
				findAll(cssSelector(".markersA input.flexi-picker")).toList.count(_.isDisplayed) should be (2)
			}
			new TextField(findAll(cssSelector(".markersA input.flexi-picker")).toList.apply(1).underlying).value = P.Marker2.usercode

			textField("markersB").value = P.Marker3.usercode
			click on className("markersB").webElement.findElement(By.cssSelector("button.btn"))

			And("I submit the form")
			cssSelector(s"input[name=editAndEditDetails]").webElement.click()
			Then("I should be back on the summary page")
			withClue(pageSource) {
				currentUrl should endWith("/summary")
			}

			When("I click on the edit button again")
			click on partialLinkText("Edit assignment")
			Then("I see the edit details screen again with all the workflow information saved")
			eventually(pageSource contains "Edit assignment details" should be {true})
			singleSel("workflowCategory").value should be(WorkflowCategory.SingleUse.code)
			singleSel("workflowType").value should be(ModeratedMarking.name)
			new TextField(findAll(cssSelector(".markersA input.flexi-picker")).toList.head.underlying).value should be (P.Marker1.usercode)
			new TextField(findAll(cssSelector(".markersA input.flexi-picker")).toList.apply(1).underlying).value should be (P.Marker2.usercode)
			new TextField(findAll(cssSelector(".markersB input.flexi-picker")).toList.head.underlying).value should be (P.Marker3.usercode)
		}
	}

}
