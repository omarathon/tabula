package uk.ac.warwick.tabula.data.model.notifications.cm2

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.Student
import uk.ac.warwick.userlookup.User

case class StudentAtStagesCount(stageName: String, count: Int)

class ReleaseToMarkerNotificationHelper(assignment: Assignment, recipient: User, cm2MarkingWorkflowService: CM2MarkingWorkflowService) {

	// all students assigned to recipient (marker) for this assignment
	lazy val studentsAllocatedToThisMarker: Set[Student] =
		if (assignment.cm2Assignment) {
			cm2MarkingWorkflowService.getAllStudentsForMarker(assignment, recipient).toSet
		} else {
			assignment.markingWorkflow.getMarkersStudents(assignment, recipient).distinct.toSet
		}

	// students at each stage that's assigned to the recipient for this assignment
	lazy val studentsAtStages: Seq[(MarkingWorkflowStage, Set[Student])] = {
		if (assignment.cm2Assignment) {
			assignment.cm2MarkingWorkflow.workflowType.allStages.map { stage =>
				stage -> {
					val allocationsForAllMarkers = cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage)
					val allocationsForThisMarker = allocationsForAllMarkers.get(recipient)
					allocationsForThisMarker.map(_.intersect(studentsAllocatedToThisMarker)).getOrElse(Set.empty)
				}
			}
		} else Seq.empty
	}

	lazy val studentsAtStagesCount: Seq[StudentAtStagesCount] = {
		studentsAtStages.map {
			case (stage, students) => StudentAtStagesCount(stage.description, students.size)
		}
	}

	lazy val submissionsCount: Int =
		if (assignment.cm2Assignment)
			assignment.cm2MarkerSubmissions(recipient).count(_.submitted)
		else
			assignment.getMarkersSubmissions(recipient).distinct.size
}
