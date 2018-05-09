package uk.ac.warwick.tabula.services.cm2

import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{DblBlndInitialMarkerA, SingleMarkingCompleted}
import uk.ac.warwick.tabula.data.model.markingworkflow.{DoubleBlindWorkflow, SingleMarkerWorkflow}
import uk.ac.warwick.tabula.helpers.cm2.{FeedbackListItem, SubmissionListItem, WorkflowItems}
import uk.ac.warwick.tabula.services.cm2.CM2WorkflowStages.{CM2MarkingWorkflowStage, ReleaseFeedback}
import uk.ac.warwick.tabula.{FeaturesImpl, Fixtures, Mockito, TestBase}

class CM2WorkflowProgressServiceTest extends TestBase with Mockito {
	val service: CM2WorkflowProgressService = new CM2WorkflowProgressService {
		features = new FeaturesImpl
	}

	@Test
	def testSingleMarking(): Unit = {
		val dept = Fixtures.department("IN")
		val assignment = Fixtures.assignment("Test")
		assignment.cm2Assignment = true
		assignment.module = Fixtures.module("IN101")
		assignment.module.adminDepartment = dept
		assignment.cm2MarkingWorkflow = SingleMarkerWorkflow.apply(name = "Test single marker workflow", department = dept, firstMarkers = Nil)

		val feedback = Fixtures.assignmentFeedback()
		feedback.assignment = assignment
		feedback.outstandingStages.add(SingleMarkingCompleted)

		val submission = Fixtures.submission()

		val workflowItems = WorkflowItems(student = Fixtures.user(), enhancedSubmission = Some(SubmissionListItem(submission, downloaded = false)), enhancedExtension = None, enhancedFeedback = Some(FeedbackListItem(feedback = feedback, downloaded = false, onlineViewed = false, null)))

		val progress = service.progress(assignment)(workflowItems)

		progress.nextStage shouldBe Some(ReleaseFeedback)
	}

	@Test
	def testDoubleBlindInitialMarkerBStageComplete(): Unit = {
		val dept = Fixtures.department("IN")
		val assignment = Fixtures.assignment("Test")
		assignment.cm2Assignment = true
		assignment.module = Fixtures.module("IN101")
		assignment.module.adminDepartment = dept
		assignment.cm2MarkingWorkflow = DoubleBlindWorkflow.apply(name = "Test double blind workflow", department = dept, initialMarkers = Nil, finalMarkers = Nil)

		val feedback = Fixtures.assignmentFeedback()
		feedback.assignment = assignment
		// Initial marker B is complete, initial marker A is still pending
		feedback.outstandingStages.add(DblBlndInitialMarkerA)

		val workflowItems = WorkflowItems(student = Fixtures.user(), enhancedSubmission = Some(SubmissionListItem(Fixtures.submission(), downloaded = false)), enhancedExtension = None, enhancedFeedback = Some(FeedbackListItem(feedback = feedback, downloaded = false, onlineViewed = false, null)))

		val progress = service.progress(assignment)(workflowItems)

		// The next stage is initial marker A, not final marker
		progress.nextStage shouldBe Some(CM2MarkingWorkflowStage(DblBlndInitialMarkerA))
	}
}
