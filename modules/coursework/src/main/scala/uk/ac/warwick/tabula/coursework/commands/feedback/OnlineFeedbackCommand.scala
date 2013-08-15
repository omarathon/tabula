package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.data.model.{Submission, Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.{FeedbackServiceComponent, SubmissionServiceComponent, AutowiringFeedbackServiceComponent, AutowiringSubmissionServiceComponent}
import scala.Some
import uk.ac.warwick.tabula.coursework.commands.feedback.StudentFeedbackGraph
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.commands.assignments.Student

object OnlineFeedbackCommand {
	def apply(module: Module, assignment: Assignment) =
		new OnlineFeedbackCommand(module, assignment)
			with ComposableCommand[Seq[StudentFeedbackGraph]]
			with OnlineFeedbackPermissions
			with AutowiringSubmissionServiceComponent
			with AutowiringFeedbackServiceComponent
			with Unaudited
			with ReadOnly
}

abstract class OnlineFeedbackCommand(val module: Module, val assignment: Assignment)
	extends CommandInternal[Seq[StudentFeedbackGraph]]
	with Appliable[Seq[StudentFeedbackGraph]] with OnlineFeedbackState {

	self: SubmissionServiceComponent with FeedbackServiceComponent =>

	def applyInternal() = {
		val students = assignment.membershipInfo.items.map(_.user)
		students.map { student =>
			val hasSubmission = submissionService.getSubmissionByUniId(assignment, student.getWarwickId).isDefined
			val feedback = feedbackService.getFeedbackByUniId(assignment, student.getWarwickId)
			val (hasFeedback, hasPublishedFeedback) = feedback match {
				case Some(f) => (true, f.released.booleanValue)
				case None => (false, false)
			}
			new StudentFeedbackGraph(student, hasSubmission, hasFeedback, hasPublishedFeedback)
		}
	}
}

trait OnlineFeedbackPermissions extends RequiresPermissionsChecking {

	self: OnlineFeedbackState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Feedback.Create, assignment)
	}
}

trait OnlineFeedbackState {
	val assignment: Assignment
	val module: Module
}


case class StudentFeedbackGraph(
	val student: User,
	val hasSubmission: Boolean,
	val hasFeedback: Boolean,
	val hasPublishedFeedback: Boolean
)