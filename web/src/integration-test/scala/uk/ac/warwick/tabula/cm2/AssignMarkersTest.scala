package uk.ac.warwick.tabula.cm2

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.{DoubleMarking, ModeratedMarking, SingleMarking}


class AssignMarkersTest  extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	"Department admin" should "be able to allocate markers to a single marker workflow " in {
		withAssignmentWithWorkflow(SingleMarking, Seq(P.Marker1, P.Marker2)) { id =>

			navigateToMarkerAllocation()

			When("I randomly assign the markers")
			click on partialLinkText("Randomly allocate")
			Then("The two markers should be assigned")
			findAll(cssSelector(".drag-count"))foreach(_.underlying.getText should be ("1"))

			When("I submit the marker allocation")
			cssSelector(s"input[name=createAndAddMarkers]").webElement.click()
			Then("I am redirected to the summary screen ")
			eventually(currentUrl should include(s"/admin/assignments/$id/summary"))

			When("I navigate back to marker allocation")
			navigateToMarkerAllocation()
			Then("The allocations have been saved")
			findAll(cssSelector(".drag-count"))foreach(_.underlying.getText should be ("1"))

			When("I remove the markers")
			click on partialLinkText("Remove all")
			Then("No markers should be assigned")
			findAll(cssSelector(".drag-count"))foreach(_.underlying.getText should be ("0"))
			cssSelector(s"input[name=createAndAddMarkers]").webElement.click()

			When("I navigate back to marker allocation gain")
			navigateToMarkerAllocation()
			Then("The allocations are still removed")
			findAll(cssSelector(".drag-count"))foreach(_.underlying.getText should be ("0"))
		}
	}

	"Department admin" should "be able to allocate markers to a double marker workflow " in {
		withAssignmentWithWorkflow(DoubleMarking, Seq(P.Marker1, P.Marker2), Seq(P.Marker3)) { id =>

			navigateToMarkerAllocation()

			findAll(partialLinkText("Randomly allocate"))

			When("I randomly assign the markers")
			findAll(partialLinkText("Randomly allocate")).toSeq.foreach(e => click on e.underlying)
			Then("The two single markers and the second marker should be assigned")
			val (first, second) = findAll(cssSelector(".drag-count")).toSeq.splitAt(2)
			first.foreach(_.underlying.getText should be ("1"))
			second.foreach(_.underlying.getText should be ("2"))

			When("I submit the marker allocation")
			cssSelector(s"input[name=createAndAddMarkers]").webElement.click()
			Then("I am redirected to the summary screen ")
			eventually(currentUrl should include(s"/admin/assignments/$id/summary"))

			When("I navigate back to marker allocation")
			navigateToMarkerAllocation()
			Then("The allocations have been saved")
			val (first1, second1) = findAll(cssSelector(".drag-count")).toSeq.splitAt(2)
			first1.foreach(_.underlying.getText should be ("1"))
			second1.foreach(_.underlying.getText should be ("2"))

			When("I remove the markers")
			findAll(partialLinkText("Remove all")).toSeq.foreach(e => click on e.underlying)
			Then("No markers should be assigned")
			findAll(cssSelector(".drag-count"))foreach(_.underlying.getText should be ("0"))
			cssSelector(s"input[name=createAndAddMarkers]").webElement.click()

			When("I navigate back to marker allocation gain")
			navigateToMarkerAllocation()
			Then("The allocations are still removed")
			findAll(cssSelector(".drag-count"))foreach(_.underlying.getText should be ("0"))
		}
	}

	"Department admin" should "be able to allocate markers to a moderated marker workflow " in {
		withAssignmentWithWorkflow(ModeratedMarking, Seq(P.Marker1, P.Marker2), Seq(P.Marker3)) { id =>

			navigateToMarkerAllocation()

			findAll(partialLinkText("Randomly allocate"))

			When("I randomly assign the markers")
			findAll(partialLinkText("Randomly allocate")).toSeq.foreach(e => click on e.underlying)
			Then("The two single markers and the second marker should be assigned")
			val (first, second) = findAll(cssSelector(".drag-count")).toSeq.splitAt(2)
			first.foreach(_.underlying.getText should be ("1"))
			second.foreach(_.underlying.getText should be ("2"))

			When("I submit the marker allocation")
			cssSelector(s"input[name=createAndAddMarkers]").webElement.click()
			Then("I am redirected to the summary screen ")
			eventually(currentUrl should include(s"/admin/assignments/$id/summary"))

			When("I navigate back to marker allocation")
			navigateToMarkerAllocation()
			Then("The allocations have been saved")
			val (first1, second1) = findAll(cssSelector(".drag-count")).toSeq.splitAt(2)
			first1.foreach(_.underlying.getText should be ("1"))
			second1.foreach(_.underlying.getText should be ("2"))

			When("I remove the markers")
			findAll(partialLinkText("Remove all")).toSeq.foreach(e => click on e.underlying)
			Then("No markers should be assigned")
			findAll(cssSelector(".drag-count"))foreach(_.underlying.getText should be ("0"))
			cssSelector(s"input[name=createAndAddMarkers]").webElement.click()

			When("I navigate back to marker allocation gain")
			navigateToMarkerAllocation()
			Then("The allocations are still removed")
			findAll(cssSelector(".drag-count"))foreach(_.underlying.getText should be ("0"))
		}
	}

}
