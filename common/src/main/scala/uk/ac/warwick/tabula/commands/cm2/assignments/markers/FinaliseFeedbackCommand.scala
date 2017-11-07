package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.coursework.FinaliseFeedbackNotification
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

/**
 * Copies the appropriate MarkerFeedback item to its parent Feedback ready for processing by administrators
 */
object FinaliseFeedbackCommand {
	def apply(assignment: Assignment, markerFeedback: Seq[MarkerFeedback], user: User) =
		new FinaliseFeedbackCommandInternal(assignment, markerFeedback, user)
			with ComposableCommand[Seq[Feedback]]
			with FinaliseFeedbackPermissions
			with FinaliseFeedbackDescription
			with FinaliseFeedbackNotifier
			with AutowiringCM2MarkingWorkflowServiceComponent
}

trait FinaliseFeedbackCommandState extends HasAssignment {
	def assignment: Assignment
	def markerFeedback: Seq[MarkerFeedback]
}

abstract class FinaliseFeedbackCommandInternal(val assignment: Assignment, val markerFeedback: Seq[MarkerFeedback], val user: User)
	extends CommandInternal[Seq[Feedback]] with FinaliseFeedbackCommandState with UserAware {

	self: CM2MarkingWorkflowServiceComponent =>

	override def applyInternal(): Seq[Feedback] = {
		markerFeedback.map(cm2MarkingWorkflowService.finaliseFeedback)
	}
}

trait FinaliseFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FinaliseFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, mandatory(assignment))
	}
}

trait FinaliseFeedbackDescription extends Describable[Seq[Feedback]] {
	self: FinaliseFeedbackCommandState =>

	override lazy val eventName: String = "FinaliseFeedback"

	override def describe(d: Description) {
		d.assignment(assignment)
		d.property("updatedFeedback" -> markerFeedback.size)
	}

}

trait FinaliseFeedbackNotifier extends Notifies[Seq[Feedback], Seq[Feedback]] {
	self: HasAssignment with UserAware =>

	override def emit(feedbacks: Seq[Feedback]): Seq[Notification[Feedback, Assignment]] = {
		Seq(Notification.init(new FinaliseFeedbackNotification, user, feedbacks.filterNot { _.checkedReleased }, assignment))
	}
}

trait FinaliseFeedbackComponent {
	def finaliseFeedback(assignment: Assignment, markerFeedback: Seq[MarkerFeedback])
}

trait FinaliseFeedbackComponentImpl extends FinaliseFeedbackComponent {

	val marker: User

	def finaliseFeedback(assignment: Assignment, markerFeedback: Seq[MarkerFeedback]) {
		val finaliseFeedbackCommand = FinaliseFeedbackCommand(assignment, markerFeedback, marker)
		finaliseFeedbackCommand.apply()
	}
}