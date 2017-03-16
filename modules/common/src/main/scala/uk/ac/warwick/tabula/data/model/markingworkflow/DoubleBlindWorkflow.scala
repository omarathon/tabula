package uk.ac.warwick.tabula.data.model.markingworkflow

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{DblBlndFinalMarker, DblBlndInitialMarkerA, DblBlndInitialMarkerB}
import uk.ac.warwick.userlookup.User

@Entity @DiscriminatorValue("dblBlind")
class DoubleBlindWorkflow extends CM2MarkingWorkflow {
	def allStages: Seq[MarkingWorkflowStage] = Seq(DblBlndInitialMarkerA, DblBlndInitialMarkerB, DblBlndFinalMarker)
	def initialStages: Seq[MarkingWorkflowStage] = Seq(DblBlndInitialMarkerA, DblBlndInitialMarkerB)
}

object DoubleBlindWorkflow {
	def apply(name: String, department: Department, initialMarkers: Seq[User], finalMarkers: Seq[User]) = {
		val workflow = new DoubleBlindWorkflow
		workflow.name = name
		workflow.department = department

		val initialAMarkers = new StageMarkers
		initialAMarkers.stage = DblBlndInitialMarkerA
		initialAMarkers.workflow = workflow

		val initialBMarkers = new StageMarkers
		initialBMarkers.stage = DblBlndInitialMarkerB
		initialBMarkers.workflow = workflow

		initialMarkers.foreach(user => {
			initialAMarkers.markers.knownType.add(user)
			initialBMarkers.markers.knownType.add(user)
		})

		val lastMarkers = new StageMarkers
		lastMarkers.stage = DblBlndFinalMarker
		lastMarkers.workflow = workflow
		finalMarkers.foreach(lastMarkers.markers.knownType.add)

		workflow
	}
}
