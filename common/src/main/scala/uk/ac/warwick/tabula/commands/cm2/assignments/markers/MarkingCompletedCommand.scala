package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.assignments.{FeedbackReleasedNotifier, ReleasedState}
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.markingworkflow.{FinalStage, MarkingWorkflowStage}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.cm2.{ReleaseToMarkerNotification, ReturnToMarkerNotification}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent, FeedbackServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._


object MarkingCompletedCommand {
	def apply(assignment: Assignment, marker: User, submitter: CurrentUser, stage: MarkingWorkflowStage) =
		new MarkingCompletedCommandInternal(assignment, marker, submitter, stage)
			with ComposableCommand[Seq[AssignmentFeedback]]
			with MarkingCompletedValidation
			with MarkingCompletedPermissions
			with MarkingCompletedDescription
			with AutowiringCM2MarkingWorkflowServiceComponent
			with FinaliseFeedbackComponentImpl
			with FeedbackReleasedNotifier
}

class MarkingCompletedCommandInternal(val assignment: Assignment, val marker: User, val submitter: CurrentUser, val stage: MarkingWorkflowStage)
	extends CommandInternal[Seq[AssignmentFeedback]] with MarkingCompletedState with ReleasedState with MarkingCompletedValidation {

	this: CM2MarkingWorkflowServiceComponent with FinaliseFeedbackComponent =>

	def applyInternal(): Seq[AssignmentFeedback] = transactional() {

		val feedback = feedbackForRelease.map(mf => HibernateHelpers.initialiseAndUnproxy(mf.feedback)).collect{ case f: AssignmentFeedback => f }
		newReleasedFeedback = cm2MarkingWorkflowService.progressFeedback(stage, feedback).asJava

		// finalise any feedback that has finished the workflow
		val toFinalise = {
			val feedbackAtFinalStage = feedback.filter(f => f.outstandingStages.asScala.collect{case s: FinalStage => s}.nonEmpty).map(_.id)
			feedbackForRelease.filter(mf => feedbackAtFinalStage.contains(mf.feedback.id))
		}
		if (toFinalise.nonEmpty) {
			finaliseFeedback(assignment, toFinalise)
		}

		feedback
	}
}

trait MarkingCompletedPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: MarkingCompletedState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait MarkingCompletedValidation extends SelfValidating {
	self: MarkingCompletedState =>
	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
		if (markerFeedback.isEmpty) errors.rejectValue("markerFeedback", "markerFeedback.finishMarking.noStudents")
	}
}

trait MarkingCompletedDescription extends Describable[Seq[AssignmentFeedback]] {

	self: MarkingCompletedState =>

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> feedbackForRelease.map(_.feedback.usercode))
	}

	override def describeResult(d: Description, result: Seq[AssignmentFeedback]){
		d.assignment(assignment)
			.property("numFeedbackUpdated" -> result.length)
	}
}

trait MarkingCompletedState extends CanProxy with UserAware {

	import uk.ac.warwick.tabula.JavaImports._

	var markerFeedback: JList[MarkerFeedback] = JArrayList()
	var confirm: Boolean = false

	val assignment: Assignment
	val marker: User
	val user: User = marker
	val submitter: CurrentUser
	val stage: MarkingWorkflowStage

	// Pre-submit validation
	def noMarks: Seq[MarkerFeedback] = markerFeedback.asScala.filter(!_.hasMark)
	def noFeedback: Seq[MarkerFeedback] = markerFeedback.asScala.filter(!_.hasFeedback)
	def noContent: Seq[MarkerFeedback] = markerFeedback.asScala.filter(!_.hasContent) // should be empty
	def releasedFeedback: Seq[MarkerFeedback] = markerFeedback.asScala.filter(mf => {
		mf.stage.order < mf.feedback.currentStageIndex
	})

	// do not update previously released feedback or feedback belonging to other markers
	lazy val feedbackForRelease: Seq[MarkerFeedback] = markerFeedback.asScala.filter(_.marker == marker) -- releasedFeedback
}

trait MarkerCompletedNotificationCompletion extends CompletesNotifications[Unit] {

	self: MarkingCompletedState with NotificationHandling with FeedbackServiceComponent =>

	def notificationsToComplete(commandResult: Unit): CompletesNotificationsResult = {
		val notificationsToComplete = feedbackForRelease
			.flatMap(mf =>
				notificationService.findActionRequiredNotificationsByEntityAndType[ReleaseToMarkerNotification](mf) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[ReturnToMarkerNotification](mf)
			)
		CompletesNotificationsResult(notificationsToComplete, marker)
	}
}