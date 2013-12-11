package uk.ac.warwick.tabula.coursework.services
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.coursework.commands.assignments.ExtensionListItem
import uk.ac.warwick.tabula.coursework.commands.assignments.SubmissionListItem
import uk.ac.warwick.tabula.coursework.commands.assignments.WorkflowItems
import uk.ac.warwick.tabula.coursework.commands.feedback.FeedbackListItem
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.data.model.MarkingMethod
import scala.collection.immutable.ListMap
import uk.ac.warwick.tabula.data.model.FileAttachment

// scalastyle:off magic.number
class CourseworkWorkflowServiceTest extends TestBase {
	
	val department = Fixtures.department("in", "IT Services")
	val module = Fixtures.module("in101", "Introduction to Web Development")
	val assignment = Fixtures.assignment("Programming Test")
	assignment.module = module
	module.department = department
	
	val service = new CourseworkWorkflowService
	service.features = Features.empty
	
	private def workflowItems(
			submission: Option[Submission]=None,
			submissionDownloaded: Boolean=false,
			feedback: Option[Feedback]=None,
			feedbackDownloaded: Boolean=false,
			extension: Option[Extension]=None,
			withinExtension: Boolean=false) =
		WorkflowItems(
			enhancedSubmission=submission map { s => SubmissionListItem(s, submissionDownloaded) }, 
			enhancedFeedback=feedback map { f => FeedbackListItem(f, feedbackDownloaded) },
			enhancedExtension=extension map { e => ExtensionListItem(e, withinExtension) }
		)
		
	@Test def getStagesForAssignment {
		import WorkflowStages._
		
		assignment.collectMarks = false
		assignment.collectSubmissions = false
		assignment.markingWorkflow = null
		
		service.features.turnitin = false
		service.features.markingWorkflows = false
		department.plagiarismDetectionEnabled = false
		
		// Default stages
		service.getStagesFor(assignment) should be (Seq(
			AddFeedback, ReleaseFeedback, DownloadFeedback
		))
		
		assignment.collectMarks = true
		
		service.getStagesFor(assignment) should be (Seq(
			AddMarks, AddFeedback, ReleaseFeedback, DownloadFeedback
		))
		
		assignment.collectSubmissions = true
		
		service.getStagesFor(assignment) should be (Seq(
			WorkflowStages.Submission, DownloadSubmission, 
			AddMarks, AddFeedback, ReleaseFeedback, DownloadFeedback
		))
		
		service.features.turnitin = true
		department.plagiarismDetectionEnabled = true
		
		service.getStagesFor(assignment) should be (Seq(
			WorkflowStages.Submission, CheckForPlagiarism, DownloadSubmission,
			AddMarks, AddFeedback, ReleaseFeedback, DownloadFeedback
		))
		
		service.features.markingWorkflows = true
		assignment.markingWorkflow = Fixtures.studentsChooseMarkerWorkflow("my workflow")
		
		service.getStagesFor(assignment) should be (Seq(
			WorkflowStages.Submission, CheckForPlagiarism, DownloadSubmission,
			ReleaseForMarking, FirstMarking,
			AddMarks, AddFeedback, ReleaseFeedback, DownloadFeedback
		))

		assignment.markingWorkflow = Fixtures.seenSecondMarkingWorkflow("my workflow")
		service.getStagesFor(assignment) should be (Seq(
			WorkflowStages.Submission, CheckForPlagiarism, DownloadSubmission,
			ReleaseForMarking, FirstMarking, SecondMarking,
			AddMarks, AddFeedback, ReleaseFeedback, DownloadFeedback
		))
	}
	
	@Test def progress {
		// Start with a very basic assignment, only the 3 core feedback workflows.
		assignment.collectMarks = false
		assignment.collectSubmissions = false
		assignment.markingWorkflow = null
		department.plagiarismDetectionEnabled = false
		
		// lines were getting a bit long...
		import WorkflowStages._
		
		{
			val p = service.progress(assignment)(workflowItems(feedback=None))
			p.stages should be (ListMap(
				"AddFeedback" -> StageProgress(AddFeedback, false, "workflow.AddFeedback.notUploaded", Good, false, true),
				"ReleaseFeedback" -> StageProgress(ReleaseFeedback, false, "workflow.ReleaseFeedback.notReleased", Good, false, false),
				"DownloadFeedback" -> StageProgress(DownloadFeedback, false, "workflow.DownloadFeedback.notDownloaded", Good, false, false)
			))
			p.percentage should be (0)
			p.nextStage should be (None) // no next stage because we haven't started
			p.messageCode should be ("workflow.AddFeedback.notUploaded")
			p.cssClass should be ("success")
		}
		
		val feedback = Fixtures.feedback("0672089")
		feedback.attachments.add(new FileAttachment)
		
		{
			val p = service.progress(assignment)(workflowItems(feedback=Some(feedback)))
			p.stages should be (ListMap(
				"AddFeedback" -> StageProgress(AddFeedback, true, "workflow.AddFeedback.uploaded", Good, true, true),
				"ReleaseFeedback" -> StageProgress(ReleaseFeedback, true, "workflow.ReleaseFeedback.notReleased", Warning, false, true),
				"DownloadFeedback" -> StageProgress(DownloadFeedback, false, "workflow.DownloadFeedback.notDownloaded", Good, false, false)
			))
			p.percentage should be (66)
			p.nextStage should be (Some(ReleaseFeedback))
			p.messageCode should be ("workflow.ReleaseFeedback.notReleased")
			p.cssClass should be ("warning")
		}
		
		feedback.released = true
		
		{
			val p = service.progress(assignment)(workflowItems(feedback=Some(feedback)))
			p.stages should be (ListMap(
				"AddFeedback" -> StageProgress(AddFeedback, true, "workflow.AddFeedback.uploaded", Good, true, true),
				"ReleaseFeedback" -> StageProgress(ReleaseFeedback, true, "workflow.ReleaseFeedback.released", Good, true, true),
				"DownloadFeedback" -> StageProgress(DownloadFeedback, true, "workflow.DownloadFeedback.notDownloaded", Warning, false, true)
			))
			p.percentage should be (100)
			p.nextStage should be (Some(DownloadFeedback))
			p.messageCode should be ("workflow.DownloadFeedback.notDownloaded")
			p.cssClass should be ("warning")
		}
		
		{
			val p = service.progress(assignment)(workflowItems(feedback=Some(feedback), feedbackDownloaded=true))
			p.stages should be (ListMap(
				"AddFeedback" -> StageProgress(AddFeedback, true, "workflow.AddFeedback.uploaded", Good, true, true),
				"ReleaseFeedback" -> StageProgress(ReleaseFeedback, true, "workflow.ReleaseFeedback.released", Good, true, true),
				"DownloadFeedback" -> StageProgress(DownloadFeedback, true, "workflow.DownloadFeedback.downloaded", Good, true, true)
			))
			p.percentage should be (100)
			p.nextStage should be (None) // Complete
			p.messageCode should be ("workflow.DownloadFeedback.downloaded")
			p.cssClass should be ("success")
		}
		
		// TODO do a fuller example that tests the submission and marking bits too
	}

}