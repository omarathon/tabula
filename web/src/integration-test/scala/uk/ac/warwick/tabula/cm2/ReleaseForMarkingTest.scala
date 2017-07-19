package uk.ac.warwick.tabula.cm2

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.SingleMarking

class ReleaseForMarkingTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	"Admin" should "be able to release for marking" in {
		withAssignmentWithWorkflow(SingleMarking, Seq(P.Marker1, P.Marker2)) { id =>
			releaseForMarking(id)
			findAll(cssSelector("td.action-col"))foreach(_.underlying.getText should be ("Submission needs marking"))
		}
	}

}
