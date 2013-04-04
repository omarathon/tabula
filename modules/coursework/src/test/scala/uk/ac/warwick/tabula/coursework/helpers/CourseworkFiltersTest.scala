package uk.ac.warwick.tabula.coursework.helpers

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

import org.joda.time.DateTime

import uk.ac.warwick.tabula.Fixtures

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.coursework.commands.assignments.ExtensionListItem
import uk.ac.warwick.tabula.coursework.commands.assignments.Student
import uk.ac.warwick.tabula.coursework.commands.assignments.SubmissionListItem
import uk.ac.warwick.tabula.coursework.commands.assignments.WorkflowItems
import uk.ac.warwick.tabula.coursework.commands.feedback.FeedbackListItem
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.MarkingMethod
import uk.ac.warwick.tabula.data.model.MarkingState
import uk.ac.warwick.tabula.data.model.OriginalityReport
import uk.ac.warwick.tabula.data.model.SavedSubmissionValue
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.forms.MarkerSelectField

class CourseworkFiltersTest extends TestBase {
	
	val department = Fixtures.department("in", "IT Services")
	val module = Fixtures.module("in101", "Introduction to Web Development")
	val assignment = Fixtures.assignment("Programming Test")
	assignment.module = module
	module.department = department
	
	@Test def of {		
		CourseworkFilters.of("NotReleasedForMarking") match {
			case CourseworkFilters.NotReleasedForMarking => 
			case what:Any => fail("what is this?" + what) 
		}
	}
	
	@Test(expected=classOf[IllegalArgumentException]) def invalidFilter {
		CourseworkFilters.of("Spank")
	}
	
	@Test def name {
		CourseworkFilters.AllStudents.getName should be ("AllStudents")
		CourseworkFilters.NotReleasedForMarking.getName should be ("NotReleasedForMarking")
		CourseworkFilters.of("NotReleasedForMarking").getName should be ("NotReleasedForMarking")
	}
	
	@Test def AllStudents {
		val filter = CourseworkFilters.AllStudents
		
		// Should pass anything and any assignment, so just check with null
		filter.applies(null) should be (true)
		filter.predicate(null) should be (true)
	}
	
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
	
	private def student(
			submission: Option[Submission]=None,
			submissionDownloaded: Boolean=false,
			feedback: Option[Feedback]=None,
			feedbackDownloaded: Boolean=false,
			extension: Option[Extension]=None,
			withinExtension: Boolean=false) =
		Student(
			user=null,
			progress=null,
			nextStage=None,
			stages=ListMap.empty,
			coursework=workflowItems(submission, submissionDownloaded, feedback, feedbackDownloaded, extension, withinExtension)
		)
	
	@Test def OnTime {
		val filter = CourseworkFilters.OnTime
		
		// Only applies to assignments that collect submissions
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (true)
		
		// Valid where there is a submission, that submission is not late, and that submission is not authorised late
		filter.predicate(student(submission=None)) should be (false)
		
		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment
		
		submission.isLate should be (false)
		submission.isAuthorisedLate should be (false)
		
		filter.predicate(student(submission=Some(submission))) should be (true)
		
		// Where submission is late, they don't fit
		assignment.setCloseDate(DateTime.now.minusDays(1))
		submission.submittedDate = DateTime.now
		
		submission.isLate should be (true)
		submission.isAuthorisedLate should be (false)
		filter.predicate(student(submission=Some(submission))) should be (false)
		
		// Authorised late isn't allowed here either
		// TODO is this right?
		
		val extension = Fixtures.extension("0672089", "cuscav")
		extension.approved = true
		extension.expiryDate = DateTime.now.plusDays(1)
		extension.assignment = assignment
		assignment.extensions.add(extension)
		
		submission.isLate should be (false)
		submission.isAuthorisedLate should be (true)
		filter.predicate(student(submission=Some(submission), extension=Some(extension), withinExtension=true)) should be (false)
	} 
	
	@Test def WithExtension {
		val filter = CourseworkFilters.WithExtension
		
		// Only applies to assignments that collect submissions and accept extensions
		assignment.allowExtensions = false
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (false)
		
		assignment.allowExtensions = true
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (true)
		
		// Valid when there is an extension
		filter.predicate(student(extension=None)) should be (false)
		filter.predicate(student(extension=Some(Fixtures.extension()))) should be (true)
	} 
	
	@Test def WithinExtension {
		val filter = CourseworkFilters.WithinExtension
		
		// Only applies to assignments that collect submissions and accept extensions
		assignment.allowExtensions = false
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (false)
		
		assignment.allowExtensions = true
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (true)
		
		// Valid only when the submission is authorised late (NOT when we are just within extension - else we wouldn't have submitted)
		filter.predicate(student(submission=None)) should be (false)
		
		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment
		
		submission.isLate should be (false)
		submission.isAuthorisedLate should be (false)
		
		filter.predicate(student(submission=Some(submission))) should be (false)
		
		// Where submission is late, they don't fit
		assignment.setCloseDate(DateTime.now.minusDays(1))
		submission.submittedDate = DateTime.now
		
		submission.isLate should be (true)
		submission.isAuthorisedLate should be (false)
		filter.predicate(student(submission=Some(submission))) should be (false)
		
		// Authorised late fits
		
		val extension = Fixtures.extension("0672089", "cuscav")
		extension.approved = true
		extension.expiryDate = DateTime.now.plusDays(1)
		extension.assignment = assignment
		assignment.extensions.add(extension)
		
		submission.isLate should be (false)
		submission.isAuthorisedLate should be (true)
		filter.predicate(student(submission=Some(submission), extension=Some(extension), withinExtension=true)) should be (true)
	} 
	
	@Test def Unsubmitted {
		val filter = CourseworkFilters.Unsubmitted
		
		// Only applies to assignments that collect submissions		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (true)
		
		// Valid when there is no submission
		filter.predicate(student(submission=None)) should be (true)
		filter.predicate(student(submission=Some(Fixtures.submission()))) should be (false)
	}
	
	@Test def NotReleasedForMarking {
		val filter = CourseworkFilters.NotReleasedForMarking
		
		// Only applies to assignments that collect submissions and have a marking workflow
		assignment.markingWorkflow = null
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (false)
		
		assignment.markingWorkflow = Fixtures.markingWorkflow("my marking workflow")
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (true)
		
		// Valid only where a submission exists and is not released for marking
		filter.predicate(student(submission=None)) should be (false)
		
		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment
		
		submission.isReleasedForMarking should be (false)
		
		filter.predicate(student(submission=Some(submission))) should be (true)
		
		// Release for marking, no longer fits
		val feedback = Fixtures.feedback("0672089")
		assignment.feedbacks.add(feedback)
		feedback.firstMarkerFeedback = Fixtures.markerFeedback(feedback)
		submission.isReleasedForMarking should be (true)
		
		filter.predicate(student(submission=Some(submission))) should be (false)
	} 
	
	@Test def NotMarked {
		val filter = CourseworkFilters.NotMarked
		
		// Only applies to assignments that collect submissions and have a marking workflow
		assignment.markingWorkflow = null
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (false)
		
		assignment.markingWorkflow = Fixtures.markingWorkflow("my marking workflow")
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (true)
		
		// Valid only where a submission exists, has been released for marking, and has a first marker
		filter.predicate(student(submission=None)) should be (false)
		
		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment
		
		submission.isReleasedForMarking should be (false)
		
		filter.predicate(student(submission=Some(submission))) should be (false)
		
		// Release for marking and make sure it has a first marker
		val feedback = Fixtures.feedback("0672089")
		assignment.feedbacks.add(feedback)
		feedback.firstMarkerFeedback = Fixtures.markerFeedback(feedback)
		submission.isReleasedForMarking should be (true)
		
		assignment.markingWorkflow.markingMethod = MarkingMethod.StudentsChooseMarker
		val f = new MarkerSelectField
		f.name = Assignment.defaultMarkerSelectorName
		assignment.addField(f)
		
		val v = new SavedSubmissionValue
		v.name = Assignment.defaultMarkerSelectorName
		v.value = "cusmab"
		submission.values.add(v)
		
		filter.predicate(student(submission=Some(submission))) should be (true)
	} 
	
	@Test def MarkedByFirst {
		val filter = CourseworkFilters.MarkedByFirst
		
		// Only applies to assignments that collect submissions and have a marking workflow
		assignment.markingWorkflow = null
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (false)
		
		assignment.markingWorkflow = Fixtures.markingWorkflow("my marking workflow")
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (true)
		
		// Valid only if released to second marker OR marking is completed
		filter.predicate(student(submission=None)) should be (false)
		
		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment
		
		submission.isReleasedToSecondMarker should be (false)
		submission.state should not be (MarkingState.MarkingCompleted)
		
		filter.predicate(student(submission=Some(submission))) should be (false)
		
		submission.state = MarkingState.MarkingCompleted
		filter.predicate(student(submission=Some(submission))) should be (true)
		
		submission.state = MarkingState.ReleasedForMarking
		
		val feedback = Fixtures.feedback("0672089")
		assignment.feedbacks.add(feedback)
		feedback.firstMarkerFeedback = Fixtures.markerFeedback(feedback)
		feedback.secondMarkerFeedback = Fixtures.markerFeedback(feedback)
		
		submission.isReleasedToSecondMarker should be (true)
		filter.predicate(student(submission=Some(submission))) should be (true)
	} 
	
	@Test def MarkedBySecond {
		val filter = CourseworkFilters.MarkedBySecond
		
		// Only applies to assignments that collect submissions and have a marking workflow, and only if it's seen second marking
		assignment.markingWorkflow = null
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (false)
		
		assignment.markingWorkflow = Fixtures.markingWorkflow("my marking workflow")
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (false)
		
		assignment.markingWorkflow.markingMethod = MarkingMethod.SeenSecondMarking
		filter.applies(assignment) should be (true)
		
		// Valid only if marking is completed
		filter.predicate(student(submission=None)) should be (false)
		
		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment
		
		submission.state should not be (MarkingState.MarkingCompleted)
		
		filter.predicate(student(submission=Some(submission))) should be (false)
		
		submission.state = MarkingState.MarkingCompleted
		
		filter.predicate(student(submission=Some(submission))) should be (true)
	}
	
	@Test def CheckedForPlagiarism {
		val filter = CourseworkFilters.CheckedForPlagiarism
		
		// Only applies to assignments that collect submissions and the department has plagiarism detection enabled
		department.plagiarismDetectionEnabled = false
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (false)
		
		department.plagiarismDetectionEnabled = true
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (true)
		
		// Valid when the submission exists and it has at least one attachment with an originality report
		filter.predicate(student(submission=None)) should be (false)
		
		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment
		
		submission.hasOriginalityReport.booleanValue() should be (false)
		
		filter.predicate(student(submission=Some(submission))) should be (false)
		
		// Release for marking, no longer fits
		val a = new FileAttachment
		a.originalityReport = new OriginalityReport
		submission.values.add(SavedSubmissionValue.withAttachments(submission, "Turnitin", Seq(a).toSet.asJava))
		
		submission.hasOriginalityReport.booleanValue() should be (true)
		
		filter.predicate(student(submission=Some(submission))) should be (true)
	} 
	
	@Test def NotCheckedForPlagiarism {
		val filter = CourseworkFilters.NotCheckedForPlagiarism
		
		// Only applies to assignments that collect submissions and the department has plagiarism detection enabled
		department.plagiarismDetectionEnabled = false
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (false)
		
		department.plagiarismDetectionEnabled = true
		
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (true)
		
		// Valid when the submission exists and it doesn't have an originality report
		filter.predicate(student(submission=None)) should be (false)
		
		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment
		
		submission.hasOriginalityReport.booleanValue() should be (false)
		
		filter.predicate(student(submission=Some(submission))) should be (true)
		
		// Release for marking, no longer fits
		val a = new FileAttachment
		a.originalityReport = new OriginalityReport
		submission.values.add(SavedSubmissionValue.withAttachments(submission, "Turnitin", Seq(a).toSet.asJava))
		
		submission.hasOriginalityReport.booleanValue() should be (true)
		
		filter.predicate(student(submission=Some(submission))) should be (false)
	} 
	
	@Test def MarkedPlagiarised {
		val filter = CourseworkFilters.MarkedPlagiarised
		
		// Only applies to assignments that collect submissions
		assignment.collectSubmissions = false
		filter.applies(assignment) should be (false)
		
		assignment.collectSubmissions = true
		filter.applies(assignment) should be (true)
		
		// Valid when the submission exists and it has been marked as plagiarised
		filter.predicate(student(submission=None)) should be (false)
		
		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment
		
		submission.suspectPlagiarised = false
		
		filter.predicate(student(submission=Some(submission))) should be (false)
		
		submission.suspectPlagiarised = true
		
		filter.predicate(student(submission=Some(submission))) should be (true)
	}
	
	@Test def NoFeedback {
		val filter = CourseworkFilters.NoFeedback
		
		// Should pass any assignment, so just check with null
		filter.applies(null) should be (true)
		
		// Valid where there's no feedback
		filter.predicate(student(feedback=None)) should be (true)
		filter.predicate(student(feedback=Some(Fixtures.feedback()))) should be (false)
	} 
	
	@Test def FeedbackNotReleased {
		val filter = CourseworkFilters.FeedbackNotReleased
		
		// Should pass any assignment, so just check with null
		filter.applies(null) should be (true)
		
		// Valid where there's feedback, but it hasn't been released
		filter.predicate(student(feedback=None)) should be (false)
		
		val feedback = Fixtures.feedback("0672089")
		feedback.released = false
		
		filter.predicate(student(feedback=Some(feedback))) should be (true)
		
		feedback.released = true
		
		filter.predicate(student(feedback=Some(feedback))) should be (false)
	} 
	
	@Test def FeedbackNotDownloaded {
		val filter = CourseworkFilters.FeedbackNotDownloaded
		
		// Should pass any assignment, so just check with null
		filter.applies(null) should be (true)
		
		// Valid where there's feedback, but it hasn't been downloaded
		filter.predicate(student(feedback=None)) should be (false)
		filter.predicate(student(feedback=Some(Fixtures.feedback()), feedbackDownloaded=false)) should be (true)
		filter.predicate(student(feedback=Some(Fixtures.feedback()), feedbackDownloaded=true)) should be (false)
	}
	

}