package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.SingleMarking

import scala.collection.JavaConverters._

class DeleteAssignmentTest extends BrowserTest with CourseworkFixtures {

	private def openAssignmentsScreen(): Unit = {

		When("I go the admin page")
		if (!currentUrl.contains("/department/xxx/20")) {
			click on linkText("Test Services")
		}

	}

	private def deleteAssignment(): Unit = {

	}

	"Department admin" should "be able to delete an assignment" in {
		withAssignment("xxx02", "Single marking-1C"){ assignmentId => }

			openAssignmentsScreen()
			deleteAssignment()

	}
}