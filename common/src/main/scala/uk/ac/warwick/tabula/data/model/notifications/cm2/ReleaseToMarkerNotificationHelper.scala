package uk.ac.warwick.tabula.data.model.notifications.cm2

import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{Assignment, FirstMarkersMap, SecondMarkersMap}
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.Student
import uk.ac.warwick.tabula.services.{CM2MarkingWorkflowService}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

case class StudentAtStagesCount(stageName: String, count: Int)

class ReleaseToMarkerNotificationHelper(assignment: Assignment, recipient: User, cm2MarkingWorkflowService: CM2MarkingWorkflowService) {

	// all students assigned to recipient (marker) for this assignment
	val allStudents: Seq[Student] = cm2MarkingWorkflowService.getAllStudentsForMarker(assignment, recipient)

	// students at each stage that's assigned to the recipient for this assignment
	def studentsAtStages: Seq[(MarkingWorkflowStage, Set[Student])] = {
		assignment.cm2MarkingWorkflow.workflowType.allStages.map { stage =>
			stage -> cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage).map {
				case (marker, students) => (marker, students.filter(allStudents.contains(_)))
			}.getOrElse(recipient, Set.empty)
		}
	}

	def studentsAtStagesCount: Seq[StudentAtStagesCount] = {
		studentsAtStages.map {
			case (stage, students) => StudentAtStagesCount(stage.name, students.size)
		}
	}
}
