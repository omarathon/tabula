package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.{FeedbackServiceComponent, SubmissionServiceComponent, AutowiringFeedbackServiceComponent, AutowiringSubmissionServiceComponent}
import scala.Some
import uk.ac.warwick.tabula.coursework.commands.feedback.StudentFeedbackGraph
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.commands.feedback.OnlineFeedbackState
import scala.Some
import uk.ac.warwick.tabula.coursework.commands.feedback.StudentFeedbackGraph
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.coursework.commands.assignments.WorkflowItems
import uk.ac.warwick.tabula.coursework.services.WorkflowStages.{Good, Warning, StageProgress}

object OnlineFeedbackFormCommand {
	def apply(module: Module, assignment: Assignment, student: Member) =
		new OnlineFeedbackFormCommand(module, assignment, student)
			with ComposableCommand[Feedback]
			with OnlineFeedbackPermissions
			with AutowiringSubmissionServiceComponent
			with AutowiringFeedbackServiceComponent
			with OnlineFeedbackFormDescription {
			override lazy val eventName = "OnlineFeedback"
		}
}

abstract class OnlineFeedbackFormCommand(val module: Module, val assignment: Assignment, val student: Member)
	extends CommandInternal[Feedback]
	with Appliable[Feedback] with OnlineFeedbackState with OnlineFeedbackStudentState {

	self: SubmissionServiceComponent with FeedbackServiceComponent =>

	def submission = submissionService.getSubmissionByUniId(assignment, student.universityId)
	def feedback = feedbackService.getFeedbackByUniId(assignment, student.universityId)
	def submissionState = {
		submission match {
			case Some(submission) if submission.isAuthorisedLate => "workflow.Submission.authorisedLate"
			case Some(submission) if submission.isLate => "workflow.Submission.late"
			case Some(_) => "workflow.Submission.onTime"
			case None if !assignment.isClosed => "workflow.Submission.unsubmitted.withinDeadline"
			case None => "workflow.Submission.unsubmitted.late"
		}
	}

	def applyInternal(): Feedback = {
		???
	}
}

trait OnlineFeedbackStudentState {
	val student: Member
}

trait OnlineFeedbackFormDescription extends Describable[Feedback] {

	this: OnlineFeedbackState with OnlineFeedbackStudentState =>

	def describe(d: Description) {
		//d.smallGroupEvent(event)
		//d.property("week", week)
	}
}