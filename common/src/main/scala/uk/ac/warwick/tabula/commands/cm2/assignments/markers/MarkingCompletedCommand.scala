package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.assignments.{FeedbackReleasedNotifier, ReleasedState}
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.markingworkflow.FinalStage
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.cm2.{ReleaseToMarkerNotification, ReturnToMarkerNotification}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent, FeedbackServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object MarkingCompletedCommand {
	def apply(assignment: Assignment, marker: User, submitter: CurrentUser, stagePosition: Int) =
		new MarkingCompletedCommandInternal(assignment, marker, submitter, stagePosition)
			with ComposableCommand[Seq[AssignmentFeedback]]
			with WorkflowProgressValidation
			with WorkflowProgressPermissions
			with MarkingCompletedDescription
			with AutowiringCM2MarkingWorkflowServiceComponent
			with FinaliseFeedbackComponentImpl
			with PopulateMarkerFeedbackComponentImpl
			with FeedbackReleasedNotifier
}

class MarkingCompletedCommandInternal(val assignment: Assignment, val marker: User, val submitter: CurrentUser, val stagePosition: Int)
	extends CommandInternal[Seq[AssignmentFeedback]] with WorkflowProgressState with ReleasedState with WorkflowProgressValidation {

	self: CM2MarkingWorkflowServiceComponent with FinaliseFeedbackComponent with PopulateMarkerFeedbackComponent =>

	def applyInternal(): Seq[AssignmentFeedback] = transactional() {

		val feedback = feedbackForRelease.map(mf => HibernateHelpers.initialiseAndUnproxy(mf.feedback)).collect{ case f: AssignmentFeedback => f }

		val feedbackForReleaseByStage = cm2MarkingWorkflowService.getAllFeedbackForMarker(assignment, marker)
			.filterKeys(_.order == stagePosition)
			.mapValues(_.filter(feedbackForRelease.contains))

		newReleasedFeedback = feedbackForReleaseByStage.flatMap{case (stage, mf) =>
			val f = mf.map(mf => HibernateHelpers.initialiseAndUnproxy(mf.feedback)).filter(_.outstandingStages.contains(stage))
			cm2MarkingWorkflowService.progress(stage, f)
		}.toSeq.asJava

		val toPopulate = newReleasedFeedback.asScala.filter(_.stage.populateWithPreviousFeedback)
		if (toPopulate.nonEmpty) {
			populateMarkerFeedback(assignment, toPopulate)
		}

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

trait WorkflowProgressPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: WorkflowProgressState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait WorkflowProgressValidation extends SelfValidating {
	self: WorkflowProgressState =>
	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
		if (markerFeedback.isEmpty) errors.rejectValue("markerFeedback", "markerFeedback.finishMarking.noStudents")
	}
}

trait MarkingCompletedDescription extends WorkflowProgressDescription {
	self: WorkflowProgressState =>
	override lazy val eventName: String = "MarkingCompleted"
}

trait WorkflowProgressDescription extends Describable[Seq[AssignmentFeedback]] {

	self: WorkflowProgressState =>

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> feedbackForRelease.map(_.feedback.usercode))
	}

	override def describeResult(d: Description, result: Seq[AssignmentFeedback]){
		d.assignment(assignment)
			.property("numFeedbackUpdated" -> result.length)
	}
}

trait WorkflowProgressState extends CanProxy with UserAware with HasAssignment {

	import uk.ac.warwick.tabula.JavaImports._

	var markerFeedback: JList[MarkerFeedback] = JArrayList()
	var confirm: Boolean = false

	val assignment: Assignment
	val marker: User
	val user: User = marker
	val submitter: CurrentUser
	val stagePosition: Int

	// Pre-submit validation
	def noContent: Seq[MarkerFeedback] = markerFeedback.asScala.filterNot(_.hasContent )
	def noMarks: Seq[MarkerFeedback] = markerFeedback.asScala.filterNot(_.hasMark) -- noContent
	def noFeedback: Seq[MarkerFeedback] = markerFeedback.asScala.filterNot(_.hasFeedback) -- noContent
	def releasedFeedback: Seq[MarkerFeedback] = markerFeedback.asScala.filter(mf => {
		mf.stage.order < mf.feedback.currentStageIndex
	})
	def notReadyToMark: Seq[MarkerFeedback] = markerFeedback.asScala.filter(mf => {
		mf.stage.order > mf.feedback.currentStageIndex
	})

	// do not update previously released feedback or feedback belonging to other markers
	lazy val feedbackForRelease: Seq[MarkerFeedback] = markerFeedback.asScala.filter(_.marker == marker) -- releasedFeedback -- notReadyToMark -- noContent
}

trait WorkflowProgressNotificationCompletion extends CompletesNotifications[Unit] {

	self: WorkflowProgressState with NotificationHandling with FeedbackServiceComponent =>

	def notificationsToComplete(commandResult: Unit): CompletesNotificationsResult = {
		val notificationsToComplete = feedbackForRelease
			.flatMap(mf =>
				notificationService.findActionRequiredNotificationsByEntityAndType[ReleaseToMarkerNotification](mf) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[ReturnToMarkerNotification](mf)
			)
		CompletesNotificationsResult(notificationsToComplete, marker)
	}
}