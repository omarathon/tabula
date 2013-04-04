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
			WorkflowStages.Submission, DownloadSubmission,
			CheckForPlagiarism,
			AddMarks, AddFeedback, ReleaseFeedback, DownloadFeedback
		))
		
		service.features.markingWorkflows = true
		assignment.markingWorkflow = Fixtures.markingWorkflow("my workflow")
		
		service.getStagesFor(assignment) should be (Seq(
			WorkflowStages.Submission, DownloadSubmission,
			CheckForPlagiarism,
			ReleaseForMarking, FirstMarking,
			AddMarks, AddFeedback, ReleaseFeedback, DownloadFeedback
		))
		
		assignment.markingWorkflow.markingMethod = MarkingMethod.SeenSecondMarking
		
		service.getStagesFor(assignment) should be (Seq(
			WorkflowStages.Submission, DownloadSubmission,
			CheckForPlagiarism,
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
		
		{
			val p = service.progress(assignment)(workflowItems(feedback=None))
			p.stages should be (ListMap(
				"AddFeedback" -> WorkflowStages.StageProgress(WorkflowStages.AddFeedback, false, "Feedback not uploaded", WorkflowStages.Good, false, true),
				"ReleaseFeedback" -> WorkflowStages.StageProgress(WorkflowStages.ReleaseFeedback, false, "Feedback not published", WorkflowStages.Good, false, false),
				"DownloadFeedback" -> WorkflowStages.StageProgress(WorkflowStages.DownloadFeedback, false, "Feedback not downloaded by student", WorkflowStages.Good, false, false)
			))
			p.percentage should be (0)
			p.nextStage should be (None) // no next stage because we haven't started
			p.message should be ("Feedback not uploaded")
			p.cssClass should be ("success")
		}
		
		val feedback = Fixtures.feedback("0672089")
		feedback.attachments.add(new FileAttachment)
		
		{
			val p = service.progress(assignment)(workflowItems(feedback=Some(feedback)))
			p.stages should be (ListMap(
				"AddFeedback" -> WorkflowStages.StageProgress(WorkflowStages.AddFeedback, true, "Feedback uploaded", WorkflowStages.Good, true, true),
				"ReleaseFeedback" -> WorkflowStages.StageProgress(WorkflowStages.ReleaseFeedback, true, "Feedback not published", WorkflowStages.Warning, false, true),
				"DownloadFeedback" -> WorkflowStages.StageProgress(WorkflowStages.DownloadFeedback, false, "Feedback not downloaded by student", WorkflowStages.Good, false, false)
			))
			p.percentage should be (66)
			p.nextStage should be (Some(WorkflowStages.ReleaseFeedback))
			p.message should be ("Feedback not published")
			p.cssClass should be ("warning")
		}
		
		feedback.released = true
		
		{
			val p = service.progress(assignment)(workflowItems(feedback=Some(feedback)))
			p.stages should be (ListMap(
				"AddFeedback" -> WorkflowStages.StageProgress(WorkflowStages.AddFeedback, true, "Feedback uploaded", WorkflowStages.Good, true, true),
				"ReleaseFeedback" -> WorkflowStages.StageProgress(WorkflowStages.ReleaseFeedback, true, "Feedback published", WorkflowStages.Good, true, true),
				"DownloadFeedback" -> WorkflowStages.StageProgress(WorkflowStages.DownloadFeedback, true, "Feedback not downloaded by student", WorkflowStages.Warning, false, true)
			))
			p.percentage should be (100)
			p.nextStage should be (Some(WorkflowStages.DownloadFeedback))
			p.message should be ("Feedback not downloaded by student")
			p.cssClass should be ("warning")
		}
		
		{
			val p = service.progress(assignment)(workflowItems(feedback=Some(feedback), feedbackDownloaded=true))
			p.stages should be (ListMap(
				"AddFeedback" -> WorkflowStages.StageProgress(WorkflowStages.AddFeedback, true, "Feedback uploaded", WorkflowStages.Good, true, true),
				"ReleaseFeedback" -> WorkflowStages.StageProgress(WorkflowStages.ReleaseFeedback, true, "Feedback published", WorkflowStages.Good, true, true),
				"DownloadFeedback" -> WorkflowStages.StageProgress(WorkflowStages.DownloadFeedback, true, "Feedback downloaded by student", WorkflowStages.Good, true, true)
			))
			p.percentage should be (100)
			p.nextStage should be (None) // Complete
			p.message should be ("Feedback downloaded by student")
			p.cssClass should be ("success")
		}
		
		// TODO do a fuller example that tests the submission and marking bits too
	}

}