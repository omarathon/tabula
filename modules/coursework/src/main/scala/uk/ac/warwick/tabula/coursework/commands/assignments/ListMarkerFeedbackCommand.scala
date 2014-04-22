package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class MarkerFeedbackItem(
	student: User,
	submission: Submission,
	feedbacks: Seq[MarkerFeedback]
)

case class MarkerFeedbackCollections(
	inProgressFeedback: Seq[MarkerFeedbackItem],
	completedFeedback: Seq[MarkerFeedbackItem],
	rejectedFeedback: Seq[MarkerFeedbackItem]
)

object ListMarkerFeedbackCommand {
	def apply(assignment:Assignment, module: Module, user:CurrentUser) =
		new ListMarkerFeedbackCommand(assignment, module, user)
		with ComposableCommand[MarkerFeedbackCollections]
		with ListMarkerFeedbackPermissions
		with ListMarkerFeedbackCommandState
		with AutowiringUserLookupComponent
		with Unaudited with ReadOnly
}

class ListMarkerFeedbackCommand(val assignment: Assignment, val module: Module, val user: CurrentUser) extends CommandInternal[MarkerFeedbackCollections] {

	self: UserLookupComponent =>

	def applyInternal() = {
		val submissions = assignment.getMarkersSubmissions(user.apparentUser)

		val (inProgressFeedback: Seq[MarkerFeedbackItem], completedFeedback: Seq[MarkerFeedbackItem], rejectedFeedback: Seq[MarkerFeedbackItem]) = {
			val markerFeedbackItems = submissions.map{ submission =>
				val student = userLookup.getUserByWarwickUniId(submission.universityId)
				val feedbacks = assignment.getAllMarkerFeedbacks(submission.universityId, user.apparentUser).reverse
				MarkerFeedbackItem(student, submission, feedbacks)
			}.filterNot(_.feedbacks.isEmpty)

			(
				markerFeedbackItems.filter(f => f.feedbacks.last.state != MarkingState.MarkingCompleted && f.feedbacks.last.state != MarkingState.Rejected),
				markerFeedbackItems.filter(f => f.feedbacks.last.state == MarkingState.MarkingCompleted),
				markerFeedbackItems.filter(f => f.feedbacks.last.state == MarkingState.Rejected)
			)
		}

		MarkerFeedbackCollections(inProgressFeedback, completedFeedback, rejectedFeedback)
	}
}

trait ListMarkerFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ListMarkerFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Feedback.Create, assignment)
	}

}

trait ListMarkerFeedbackCommandState {
	def assignment: Assignment
	def module: Module
	def user: CurrentUser
}
