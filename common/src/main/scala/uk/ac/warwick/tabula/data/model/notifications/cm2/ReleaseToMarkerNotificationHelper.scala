package uk.ac.warwick.tabula.data.model.notifications.cm2

import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{Assignment, FirstMarkersMap, SecondMarkersMap}
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.Student
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowServiceComponent
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class ReleaseToMarkerNotificationHelper(assignment: Assignment, recipient: User) {

	self: CM2MarkingWorkflowServiceComponent =>

	// all
	val allStudents: Seq[Student] = cm2MarkingWorkflowService.getAllStudentsForMarker(assignment, recipient)

	def studentsAtStages: Seq[(MarkingWorkflowStage, Set[Student])] = {
		assignment.cm2MarkingWorkflow.workflowType.allStages.map { stage =>
			stage -> cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage).map {
				case (marker, students) => (marker, students.filter(allStudents.contains(_)))
			}.getOrElse(recipient, Set.empty)
		}
	}
}
