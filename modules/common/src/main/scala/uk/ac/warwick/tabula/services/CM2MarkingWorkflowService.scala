package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.springframework.stereotype.Service

import scala.collection.JavaConverters._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.AutowiringCM2MarkingWorkflowDaoComponent
import uk.ac.warwick.tabula.data.model.markingworkflow.{CM2MarkingWorkflow, MarkingWorkflowStage, StageMarkers}
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, MarkerFeedback}
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.{SortedMap, TreeMap}


trait CM2MarkingWorkflowService extends WorkflowUserGroupHelpers {

	type Marker = User
	type Student = User

	def save(workflow: CM2MarkingWorkflow): Unit
	def releaseFeedback(feedbacks: Seq[AssignmentFeedback]): Seq[AssignmentFeedback]
	def progressFeedback(markerStage: MarkingWorkflowStage, feedbacks: Seq[AssignmentFeedback]): Seq[AssignmentFeedback]
	// essentially an undo for progressFeedback if it was done in error - not a normal step in the workflow
	def returnFeedback(feedbacks: Seq[AssignmentFeedback]): Seq[AssignmentFeedback]

	// add new markers for a workflow stage
	def addMarkersForStage(workflow: CM2MarkingWorkflow, markerStage: MarkingWorkflowStage, markers: Seq[Marker]): Unit
	// remove the specified markers from a stage - they cannot have any existing marker feedback
	def removeMarkersForStage(workflow: CM2MarkingWorkflow, markerStage: MarkingWorkflowStage, markers: Seq[Marker]): Unit
	// for a given assignment and workflow stage specify the markers for each student
	def allocateMarkersForStage(
		assignment: Assignment,
		markerStage: MarkingWorkflowStage,
		allocations: Map[Marker, Set[Student]]
	): Seq[MarkerFeedback]

	// an anonymous Marker may be present in the map if a marker has been unassigned - these need to be handled
	def getMarkerAllocations(assignment: Assignment, stage: MarkingWorkflowStage): Map[Marker, Set[Student]]
	// an anonymous Marker may be present in the map if a marker has been unassigned - these need to be handled
	def feedbackByMarker(assignment: Assignment, stage: MarkingWorkflowStage): Map[Marker, Seq[MarkerFeedback]]
	// all the marker feedback for this feedback keyed and sorted by workflow stage
	def markerFeedbackForFeedback(feedback: AssignmentFeedback): SortedMap[MarkingWorkflowStage, MarkerFeedback]

	def getAllFeedbackForMarker(assignment: Assignment, marker: User): SortedMap[MarkingWorkflowStage, Seq[MarkerFeedback]]
}

@Service
class CM2MarkingWorkflowServiceImpl extends CM2MarkingWorkflowService with AutowiringFeedbackServiceComponent
	with WorkflowUserGroupHelpersImpl with AutowiringCM2MarkingWorkflowDaoComponent {

	override def save(workflow: CM2MarkingWorkflow): Unit = markingWorkflowDao.saveOrUpdate(workflow)

	override def releaseFeedback(feedbacks: Seq[AssignmentFeedback]): Seq[AssignmentFeedback] = feedbacks.map(f => {
		f.outstandingStages = f.assignment.cm2MarkingWorkflow.initialStages.asJava
		feedbackService.saveOrUpdate(f)
		f
	})

	override def progressFeedback(currentStage: MarkingWorkflowStage, feedbacks: Seq[AssignmentFeedback]): Seq[AssignmentFeedback] = feedbacks.map(f => {
		if(currentStage.nextStages.isEmpty)
			throw new IllegalArgumentException("cannot progress feedback past the final stage")

		val remainingStages = f.outstandingStages.asScala diff Seq(currentStage)
		f.outstandingStages = if(remainingStages.isEmpty) currentStage.nextStages.asJava else remainingStages.asJava
		feedbackService.saveOrUpdate(f)
		f
	})

	override def returnFeedback(feedbacks: Seq[AssignmentFeedback]): Seq[AssignmentFeedback] = feedbacks.map(f => {
		val previousStages = f.outstandingStages.asScala.head.previousStages
		if(previousStages.isEmpty)
			throw new IllegalArgumentException("cannot return feedback past the initial stage")

		f.outstandingStages = previousStages.asJava
		feedbackService.saveOrUpdate(f)
		f
	})


	override def addMarkersForStage(workflow: CM2MarkingWorkflow, stage: MarkingWorkflowStage, markers: Seq[Marker]): Unit = {
		val markersForStage = workflow.stageMarkers.asScala.find(_.stage == stage).getOrElse(
			new StageMarkers(stage, workflow)
		)
		// usergroup handles dupes for us :)
		markers.foreach(markersForStage.markers.add)
		markingWorkflowDao.saveOrUpdate(markersForStage)
	}

	override def removeMarkersForStage(workflow: CM2MarkingWorkflow, stage: MarkingWorkflowStage, markers: Seq[Marker]): Unit = {
		val markersForStage = workflow.stageMarkers.asScala.find(_.stage == stage).getOrElse(
			throw new IllegalArgumentException("Can't remove markers for this stage as none exist")
		)
		markers.foreach(marker => {
			val existing = workflow.assignments.asScala.flatMap(a => feedbackByMarker(a, stage).getOrElse(marker, Nil))
			if (existing.nonEmpty)
				throw new IllegalArgumentException(s"Can't remove marker ${marker.getUserId} for this stage as they have marker feedback in progress")
			markersForStage.markers.remove(marker)
		})
		markingWorkflowDao.saveOrUpdate(markersForStage)
	}

	// for a given assignment and workflow stage specify the markers for each student
	override def allocateMarkersForStage(assignment: Assignment, stage: MarkingWorkflowStage, allocations: Map[Marker, Set[Student]]): Seq[MarkerFeedback] = {

		val workflow = assignment.cm2MarkingWorkflow
		if (workflow == null) throw new IllegalArgumentException("Can't assign markers for an assignment with no workflow")

		val existingMarkerFeedback = allMarkerFeedbackForStage(assignment, stage)

		// if any students did have a marker now don't remove the marker ID
		existingMarkerFeedback
			.filter(mf => allocations.getOrElse(mf.marker, Set()).isEmpty)
			.foreach(mf => {
				mf.marker = null
				feedbackService.save(mf)
			})

		for((marker, students) <- allocations.toSeq; student <- students) yield {

			val parentFeedback = assignment.feedbacks.asScala.find(_.usercode == student.getUserId).getOrElse({
				val newFeedback = new AssignmentFeedback
				newFeedback.assignment = assignment
				newFeedback.uploaderId = marker.getUserId
				newFeedback.usercode = student.getUserId
				newFeedback._universityId = student.getWarwickId
				newFeedback.released = false
				newFeedback.createdDate = DateTime.now
				feedbackService.saveOrUpdate(newFeedback)
				newFeedback
			})

			val markerFeedback = existingMarkerFeedback.find(_.student == student).getOrElse({
				val newMarkerFeedback = new MarkerFeedback(parentFeedback)
				newMarkerFeedback.stage = stage
				newMarkerFeedback
			})

			// set the marker (possibly moving the MarkerFeedback to another marker - any existing data remains)
			markerFeedback.marker = marker
			feedbackService.save(markerFeedback)
			markerFeedback
		}
	}

	private def allMarkerFeedbackForStage(assignment: Assignment, stage: MarkingWorkflowStage): Seq[MarkerFeedback] =
		markingWorkflowDao.markerFeedbackForAssignmentAndStage(assignment, stage)

	override def getMarkerAllocations(assignment: Assignment, stage: MarkingWorkflowStage): Map[Marker, Set[Student]] = {
		feedbackByMarker(assignment,stage).map{ case (marker, markerFeedbacks) =>
			marker -> markerFeedbacks.map(_.student).toSet
		}
	}

	// marker can be an Anon marker if marking
	override def feedbackByMarker(assignment: Assignment, stage: MarkingWorkflowStage): Map[Marker, Seq[MarkerFeedback]] = {
		allMarkerFeedbackForStage(assignment,stage).groupBy(_.marker)
	}

	override def markerFeedbackForFeedback(feedback: AssignmentFeedback): SortedMap[MarkingWorkflowStage, MarkerFeedback] = {
		val unsortedMap = markingWorkflowDao.markerFeedbackForFeedback(feedback)
			.groupBy(_.stage)
			.map{case (k, v) => k -> v.head}

		TreeMap(unsortedMap.toSeq:_*)
	}

	override def getAllFeedbackForMarker(assignment: Assignment, marker: User): SortedMap[MarkingWorkflowStage, Seq[MarkerFeedback]] = {
		val unsortedMap = markingWorkflowDao.markerFeedbackForMarker(assignment, marker).groupBy(_.stage)
		TreeMap(unsortedMap.toSeq:_*)
	}

}

trait WorkflowUserGroupHelpers {
	val markerHelper: UserGroupMembershipHelper[CM2MarkingWorkflow]
}

trait WorkflowUserGroupHelpersImpl extends WorkflowUserGroupHelpers {
	val markerHelper = new UserGroupMembershipHelper[CM2MarkingWorkflow]("_markers")
}

trait CM2MarkingWorkflowServiceComponent {
	def cm2MarkingWorkflowService: CM2MarkingWorkflowService
}

trait AutoWiringCM2MarkingWorkflowServiceComponent {
	def cm2MarkingWorkflowService: CM2MarkingWorkflowService =  Wire.auto[CM2MarkingWorkflowService]
}