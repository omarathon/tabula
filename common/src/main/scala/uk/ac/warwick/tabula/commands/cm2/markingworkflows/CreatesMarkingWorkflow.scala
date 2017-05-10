package uk.ac.warwick.tabula.commands.cm2.markingworkflows

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.{DoubleBlindMarking, DoubleMarking, ModeratedMarking, SingleMarking}
import uk.ac.warwick.tabula.data.model.markingworkflow._
import uk.ac.warwick.userlookup.User


trait CreatesMarkingWorkflow {

	case class MarkingWorkflowData (
		department: Department,
		workflowName: String,
		markersAUsers: Seq[User],
		markersBUsers: Seq[User],
		workflowType: MarkingWorkflowType
	)

	def createWorkflow(data: MarkingWorkflowData) : CM2MarkingWorkflow = {
		data.workflowType match {
			case DoubleMarking => DoubleWorkflow(data.workflowName, data.department, data.markersAUsers, data.markersBUsers)
			case ModeratedMarking => ModeratedWorkflow(data.workflowName, data.department, data.markersAUsers, data.markersBUsers)
			case SingleMarking => SingleMarkerWorkflow(data.workflowName, data.department, data.markersAUsers)
			case DoubleBlindMarking => DoubleBlindWorkflow(data.workflowName, data.department, data.markersAUsers, data.markersBUsers)
			case _ => throw new UnsupportedOperationException(data.workflowType + " not specified")
		}
	}

}
