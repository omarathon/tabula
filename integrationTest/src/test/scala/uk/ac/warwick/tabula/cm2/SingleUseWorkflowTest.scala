package uk.ac.warwick.tabula.cm2

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.data.model.WorkflowCategory
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.{DoubleMarking, ModeratedMarking, SingleMarking}

class SingleUseWorkflowTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	"Department admin" should "be able to create a single use single marker workflow" in {
		withAssignmentWithWorkflow(SingleMarking, Seq(P.Marker1, P.Marker2)) { _ =>
			When("I click on the edit button again")
			click on partialLinkText("Edit assignment")
			Then("I see the edit details screen again with all the workflow information saved")
			eventually(pageSource contains "Edit assignment details" should be {
				true
			})
			singleSel("workflowCategory").value should be(WorkflowCategory.SingleUse.code)
			singleSel("workflowType").value should be(SingleMarking.name)
			new TextField(findAll(cssSelector("input.flexi-picker")).toList.head.underlying).value should be(P.Marker1.usercode)
			new TextField(findAll(cssSelector("input.flexi-picker")).toList.apply(1).underlying).value should be(P.Marker2.usercode)
		}
	}

	/** This test fails sometimes - we have no idea why? MarkerA and MarkerB gets markers in the reverse order.
    	So markerA field gets markerB field's markers and vice versa
		 We did debug this but could find no issue with the test. Interestingly, debug messages pointed out
		 markers added in the right order but when we verified screen shot it was in the other order. Happens occasionally.
		 Have tweaked the test now  with different allocation to markers but may be this will get the same issue? **/
	"Department admin" should "be able to create a single use double seen marking workflow" in {
		withAssignmentWithWorkflow(DoubleMarking, Seq(P.Marker2), Seq(P.Marker1, P.Marker3)) { _ =>
		When("I click on the edit button again")
			click on partialLinkText("Edit assignment")
			Then("I see the edit details screen again with all the workflow information saved")
			eventually(pageSource contains "Edit assignment details" should be {
				true
			})
			singleSel("workflowCategory").value should be(WorkflowCategory.SingleUse.code)
			singleSel("workflowType").value should be(DoubleMarking.name)
			new TextField(findAll(cssSelector(".markersA input.flexi-picker")).toList.head.underlying).value should be(P.Marker2.usercode)
			new TextField(findAll(cssSelector(".markersB input.flexi-picker")).toList.head.underlying).value should be(P.Marker1.usercode)
			new TextField(findAll(cssSelector(".markersB input.flexi-picker")).toList.apply(1).underlying).value should be(P.Marker3.usercode)
		}
	}

	"Department admin" should "be able to create a single use moderated marking workflow" in {
		withAssignmentWithWorkflow(ModeratedMarking, Seq(P.Marker1, P.Marker2), Seq(P.Marker3)) { _ =>
			When("I click on the edit button again")
			click on partialLinkText("Edit assignment")
			Then("I see the edit details screen again with all the workflow information saved")
			eventually(pageSource contains "Edit assignment details" should be {
				true
			})
			singleSel("workflowCategory").value should be(WorkflowCategory.SingleUse.code)
			singleSel("workflowType").value should be(ModeratedMarking.name)
			new TextField(findAll(cssSelector(".markersA input.flexi-picker")).toList.head.underlying).value should be(P.Marker1.usercode)
			new TextField(findAll(cssSelector(".markersA input.flexi-picker")).toList.apply(1).underlying).value should be(P.Marker2.usercode)
			new TextField(findAll(cssSelector(".markersB input.flexi-picker")).toList.head.underlying).value should be(P.Marker3.usercode)
		}
	}
}
