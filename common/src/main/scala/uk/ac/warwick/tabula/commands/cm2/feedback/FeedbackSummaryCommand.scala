package uk.ac.warwick.tabula.commands.cm2.feedback

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, Feedback}
import uk.ac.warwick.tabula.services.{AutowiringFeedbackServiceComponent, FeedbackServiceComponent}
import FeedbackSummaryCommand._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

object FeedbackSummaryCommand {
	type Result = Option[AssignmentFeedback]
	type Command = Appliable[Result]

	def apply(assignment: Assignment, student: User): Command =
		new FeedbackSummaryCommandInternal(assignment, student)
			with ComposableCommand[Result]
			with FeedbackSummaryPermissions
			with FeedbackSummaryDescription
			with AutowiringFeedbackServiceComponent
			with ReadOnly
}

trait FeedbackSummaryState {
	def assignment: Assignment
	def student: User
}

class FeedbackSummaryCommandInternal(val assignment: Assignment, val student: User)
	extends CommandInternal[Result]
		with FeedbackSummaryState {
	self: FeedbackServiceComponent =>

	override def applyInternal(): Option[AssignmentFeedback] =
		feedbackService.getAssignmentFeedbackByUsercode(assignment, student.getUserId)
}

trait FeedbackSummaryPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FeedbackSummaryState =>

	override def permissionsCheck(p: PermissionsChecking): Unit =
		p.PermissionCheck(Permissions.AssignmentFeedback.Read, mandatory(assignment))
}

trait FeedbackSummaryDescription extends Describable[Result] {
	self: FeedbackSummaryState =>

	override lazy val eventName: String = "FeedbackSummary"

	override def describe(d: Description): Unit =
		d.assignment(assignment)
		 .studentIds(Option(student.getWarwickId).toSeq)
		 .studentUsercodes(student.getUserId)
}