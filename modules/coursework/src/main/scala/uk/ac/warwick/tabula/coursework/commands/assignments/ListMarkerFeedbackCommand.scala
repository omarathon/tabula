package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class MarkerFeedbackItem (
	student: User,
	submission: Submission,
	feedbacks: Seq[MarkerFeedback],
	nextMarker: Option[User]
)

case class MarkerFeedbackStage (
	roleName: String,
	nextRoleName: String,
	previousRoleName: Option[String],
	position: FeedbackPosition,
	feedbackItems: Seq[MarkerFeedbackItem]
)

object ListMarkerFeedbackCommand  {
	def apply(assignment:Assignment, module: Module, marker:User, submitter: CurrentUser) =
		new ListMarkerFeedbackCommand(assignment, module, marker, submitter)
		with ComposableCommand[Seq[MarkerFeedbackStage]]
		with ListMarkerFeedbackPermissions
		with ListMarkerFeedbackCommandState
		with AutowiringUserLookupComponent
		with Unaudited with ReadOnly
}

class ListMarkerFeedbackCommand(val assignment: Assignment, val module: Module, val marker: User, val submitter: CurrentUser)
	extends CommandInternal[Seq[MarkerFeedbackStage]] {

	self: UserLookupComponent =>

	def applyInternal() = {
		val submissions = assignment.getMarkersSubmissions(marker)
		val workflow = assignment.markingWorkflow

		val feedbackItems = submissions.map(submission => {
			val student = userLookup.getUserByWarwickUniId(submission.universityId)
			val feedbacks = assignment.getAllMarkerFeedbacks(submission.universityId, marker).reverse
			val position = feedbacks.last.getFeedbackPosition
			val nextMarker = workflow.getNextMarker(position, assignment, submission.universityId)
			MarkerFeedbackItem(student, submission, feedbacks, nextMarker)
		}).filterNot(_.feedbacks.isEmpty)

		feedbackItems
			.groupBy(_.feedbacks.last.getFeedbackPosition)
			.map { case (position, items) =>
				val roleName = workflow.getRoleNameForPosition(position)
				val nextRoleName = workflow.getRoleNameForNextPosition(position).toLowerCase
				val previousRoleName = workflow.getRoleNameForPreviousPosition(position).map(_.toLowerCase)
				MarkerFeedbackStage(roleName, nextRoleName, previousRoleName, position, items)
			}
			.toSeq
			.sortBy(_.position)
	}

}

trait ListMarkerFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ListMarkerFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Feedback.Create, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}

}

trait ListMarkerFeedbackCommandState {
	def assignment: Assignment
	def module: Module
	def marker:User
	def submitter: CurrentUser
}
