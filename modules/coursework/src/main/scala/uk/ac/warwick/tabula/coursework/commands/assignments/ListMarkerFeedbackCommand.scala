package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.helpers.{MarkerFeedbackCollections, MarkerFeedbackCollecting}
import uk.ac.warwick.tabula.data.model._
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

object ListMarkerFeedbackCommand  {
	def apply(assignment:Assignment, module: Module, marker:User, submitter: CurrentUser) =
		new ListMarkerFeedbackCommand(assignment, module, marker, submitter)
		with ComposableCommand[MarkerFeedbackCollections]
		with ListMarkerFeedbackPermissions
		with ListMarkerFeedbackCommandState
		with AutowiringUserLookupComponent
		with Unaudited with ReadOnly
}

class ListMarkerFeedbackCommand(val assignment: Assignment, val module: Module, val marker: User, val submitter: CurrentUser)
	extends CommandInternal[MarkerFeedbackCollections] with MarkerFeedbackCollecting {

	self: UserLookupComponent =>

	def applyInternal() = getMarkerFeedbackCollections(assignment, module, marker, userLookup)

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
