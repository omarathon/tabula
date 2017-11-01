package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.notifications.cm2.{ReleaseToMarkerNotification, ReleaseToMarkerNoSubmissionsNotification, ReturnToMarkerNotification}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._


object ReturnToMarkerCommand {
	def apply(assignment: Assignment, user: CurrentUser) = new ReturnToMarkerCommandInternal(assignment, user)
		with ComposableCommand[Seq[AssignmentFeedback]]
		with ReturnToMarkerValidation
		with ReturnToMarkerPermissions
		with ReturnToMarkerDescription
		with ReturnToMarkerNotifier
		with AutowiringCM2MarkingWorkflowServiceComponent
}

class ReturnToMarkerCommandInternal(val assignment: Assignment, val currentUser: CurrentUser)
	extends CommandInternal[Seq[AssignmentFeedback]] with ReturnToMarkerState with ReturnToMarkerRequest with ReleasedState {

	self: CM2MarkingWorkflowServiceComponent  =>

	def applyInternal(): Seq[AssignmentFeedback] = {

		// only move feedback backwards in the workflow
		val feedbackToReturn = feedbacks.filter(f => {
			val targetIndex = targetStages.asScala.headOption.map(_.order).getOrElse(0)
			targetIndex < f.currentStageIndex
		})

		returnedMarkerFeedback = cm2MarkingWorkflowService.returnFeedback(targetStages.asScala, feedbackToReturn).asJava
		feedbackToReturn
	}
}

trait ReturnToMarkerPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ReturnToMarkerState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
	}
}

trait ReturnToMarkerValidation extends SelfValidating {
	self: ReturnToMarkerRequest =>
	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "return.marking.confirm")
		if (targetStages.isEmpty) errors.rejectValue("targetStages", "return.marking.targetStages")
	}
}

trait ReturnToMarkerDescription extends Describable[Seq[AssignmentFeedback]] {
	self: ReturnToMarkerState with ReturnToMarkerRequest =>

	override lazy val eventName: String = "ReturnToMarker"

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> students)
	}

	override def describeResult(d: Description, result: Seq[AssignmentFeedback]){
		d.assignment(assignment)
			.property("feedbackReturned" -> result.size)
	}
}

trait ReturnToMarkerState extends SelectedStudentsState with UserAware {
	def assignment: Assignment
	def currentUser: CurrentUser
	val user: User = currentUser.apparentUser
}

trait ReturnToMarkerRequest extends SelectedStudentsRequest {
	self: ReturnToMarkerState =>
	var confirm: Boolean = false
	var targetStages: JList[MarkingWorkflowStage] = JArrayList()
	var returnedMarkerFeedback: JList[MarkerFeedback] = JArrayList()

	def published: Seq[String] = feedbacks.filter(_.released).map(_.usercode)
}


trait ReturnToMarkerNotifier extends Notifies[Seq[AssignmentFeedback], Seq[MarkerFeedback]] {

	self: ReturnToMarkerRequest with ReturnToMarkerState =>

	def emit(commandResult: Seq[AssignmentFeedback]): Seq[Notification[MarkerFeedback, Assignment]] = {

		// emit notifications to each marker that has new feedback
		val markerMap : Map[String, Seq[MarkerFeedback]] = returnedMarkerFeedback.asScala.groupBy(_.marker.getUserId)

		markerMap.map{ case (usercode, markerFeedback) =>
			val notification = Notification.init(new ReturnToMarkerNotification, user, markerFeedback, assignment)
			notification.recipientUserId = usercode
			notification
		}.toSeq
	}
}

trait ReturnToMarkerNotificationCompletion extends CompletesNotifications[Seq[AssignmentFeedback]] {

	self: ReturnToMarkerRequest with ReturnToMarkerState with NotificationHandling =>

	def notificationsToComplete(commandResult: Seq[AssignmentFeedback]): CompletesNotificationsResult = {
		val notificationsToComplete = returnedMarkerFeedback.asScala.flatMap(mf =>
				notificationService.findActionRequiredNotificationsByEntityAndType[ReleaseToMarkerNotification](mf) ++
				notificationService.findActionRequiredNotificationsByEntityAndType[ReturnToMarkerNotification](mf)
		)
		CompletesNotificationsResult(notificationsToComplete, user)
	}
}