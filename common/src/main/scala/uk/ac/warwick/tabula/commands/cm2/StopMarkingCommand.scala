package uk.ac.warwick.tabula.commands.cm2

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports.{JArrayList, _}
import uk.ac.warwick.tabula.commands.cm2.assignments.{SelectedStudentsRequest, SelectedStudentsState}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.FinalStage
import uk.ac.warwick.tabula.data.model.notifications.cm2.{ReleaseToMarkerNotification, ReturnToMarkerNotification, StopMarkingNotification}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._


object StopMarkingMarkingCommand {
	def apply(assignment: Assignment, user: CurrentUser) = new StopMarkingMarkingCommandInternal(assignment, user)
		with ComposableCommand[Seq[AssignmentFeedback]]
		with StopMarkingValidation
		with StopMarkingPermissions
		with StopMarkingDescription
		with StopMarkingNotifier
		with StopMarkingNotificationCompletion
		with AutowiringCM2MarkingWorkflowServiceComponent
}

class StopMarkingMarkingCommandInternal(val assignment: Assignment, val currentUser: CurrentUser)
	extends CommandInternal[Seq[AssignmentFeedback]] with StopMarkingState with StopMarkingRequest {

	self: CM2MarkingWorkflowServiceComponent  =>

	def applyInternal(): Seq[AssignmentFeedback] = {
		val feedbackToStop = feedbacks.filterNot(f => studentsAlreadyFinished.contains(f.usercode))
		stoppedMarkerFeedback = feedbackToStop.flatMap(_.markingInProgress).asJava
		cm2MarkingWorkflowService.stopMarking(feedbackToStop)
	}
}

trait StopMarkingPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: StopMarkingState =>

	def permissionsCheck(p: PermissionsChecking) {
		// use the same permissions as release
		p.PermissionCheck(Permissions.Submission.ReleaseForMarking, assignment)
	}
}

trait StopMarkingValidation extends SelfValidating {
	self: StopMarkingRequest =>
	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "stop.marking.confirm")
	}
}

trait StopMarkingDescription extends Describable[Seq[AssignmentFeedback]] {
	self: StopMarkingState with StopMarkingRequest =>

	override lazy val eventName: String = "StopMarking"

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> students)
	}

	override def describeResult(d: Description, result: Seq[AssignmentFeedback]){
		d.assignment(assignment)
			.property("feedbackUnReleased" -> result.size)
	}
}

trait StopMarkingState extends SelectedStudentsState with UserAware {
	def assignment: Assignment
	def currentUser: CurrentUser
	val user: User = currentUser.apparentUser
}

trait StopMarkingRequest extends SelectedStudentsRequest {
	self: StopMarkingState =>
	var confirm: Boolean = false
	var stoppedMarkerFeedback: JList[MarkerFeedback] = JArrayList()

	def studentsAlreadyFinished: Seq[String] = feedbacks.filter(f =>
		f.outstandingStages.asScala.collect{ case s: FinalStage => s }.nonEmpty
	).map(_.usercode)

}


trait StopMarkingNotifier extends Notifies[Seq[AssignmentFeedback], Seq[MarkerFeedback]] {

	self: StopMarkingRequest with StopMarkingState =>

	def emit(commandResult: Seq[AssignmentFeedback]): Seq[Notification[MarkerFeedback, Assignment]] = {
		// emit notifications to each marker that has new feedback
		val markerMap : Map[String, Seq[MarkerFeedback]] = stoppedMarkerFeedback.asScala.groupBy(_.marker.getUserId)

		markerMap.map{ case (usercode, markerFeedback) =>
			val notification = Notification.init(new StopMarkingNotification, user, markerFeedback, assignment)
			notification.recipientUserId = usercode
			notification
		}.toSeq
	}
}

trait StopMarkingNotificationCompletion extends CompletesNotifications[Seq[AssignmentFeedback]] {

	self: StopMarkingRequest with StopMarkingState with NotificationHandling =>

	def notificationsToComplete(commandResult: Seq[AssignmentFeedback]): CompletesNotificationsResult = {
		val notificationsToComplete = stoppedMarkerFeedback.asScala.flatMap(mf =>
				notificationService.findActionRequiredNotificationsByEntityAndType[ReleaseToMarkerNotification](mf) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[ReturnToMarkerNotification](mf)
		)
		CompletesNotificationsResult(notificationsToComplete, user)
	}
}