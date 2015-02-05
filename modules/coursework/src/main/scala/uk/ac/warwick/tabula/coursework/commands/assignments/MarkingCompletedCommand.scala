package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.coursework.commands.feedback.GeneratesGradesFromMarks
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.data.model.notifications.coursework.{ReturnToMarkerNotification, ModeratorRejectedNotification, ReleaseToMarkerNotification}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications.{ReleasedState, FeedbackReleasedNotifier}
import uk.ac.warwick.tabula.helpers.Logging
import scala.collection.mutable
import uk.ac.warwick.tabula.system.BindListener

object MarkingCompletedCommand {
	def apply(module: Module, assignment: Assignment, marker: User, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new MarkingCompletedCommand(module, assignment, marker, submitter, gradeGenerator)
			with ComposableCommand[Unit]
			with MarkingCompletedCommandPermissions
			with MarkingCompletedDescription
			with SecondMarkerReleaseNotifier
			with AutowiringUserLookupComponent
			with AutowiringStateServiceComponent
			with AutowiringFeedbackServiceComponent
			with MarkerCompletedNotificationCompletion
}

abstract class MarkingCompletedCommand(val module: Module, val assignment: Assignment, val user: User, val submitter: CurrentUser, val gradeGenerator: GeneratesGradesFromMarks)
	extends CommandInternal[Unit] with Appliable[Unit] with SelfValidating with UserAware with MarkingCompletedState with ReleasedState with BindListener {

	self: StateServiceComponent with FeedbackServiceComponent =>

	override def onBind(result: BindingResult) {
		// filter out any feedbacks where the current user is not the marker
		markerFeedback = markerFeedback.filter(_.getMarkerUser == user)
	}

	def preSubmitValidation() {
		noMarks = markerFeedback.filter(!_.hasMark)
		noFeedback = markerFeedback.filter(!_.hasFeedback)
		releasedFeedback = markerFeedback.filter(_.state == MarkingState.MarkingCompleted)
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
		if (markerFeedback.isEmpty) errors.rejectValue("students", "markerFeedback.finishMarking.noStudents")
	}

	def applyInternal() {
		// do not update previously released feedback
		val feedbackForRelease = markerFeedback -- releasedFeedback

		feedbackForRelease.foreach(stateService.updateState(_, MarkingState.MarkingCompleted))

		releaseNextMarkerFeedbackOrFinalise(feedbackForRelease)
	}

	private def releaseNextMarkerFeedbackOrFinalise(feedbackForRelease: mutable.Buffer[MarkerFeedback]) {
		newReleasedFeedback = feedbackForRelease.flatMap(nextMarkerFeedback).map{ nextMarkerFeedback =>
			stateService.updateState(nextMarkerFeedback, MarkingState.ReleasedForMarking)
			feedbackService.save(nextMarkerFeedback)
			nextMarkerFeedback
		}

		val feedbackToFinalise = feedbackForRelease.filter(!nextMarkerFeedback(_).isDefined)
		if (feedbackToFinalise.nonEmpty)
			finaliseFeedback(feedbackToFinalise)
	}

	def nextMarkerFeedback(markerFeedback: MarkerFeedback): Option[MarkerFeedback] = {
		markerFeedback.getFeedbackPosition match {
			case FirstFeedback if markerFeedback.feedback.assignment.markingWorkflow.hasSecondMarker =>
				Option(markerFeedback.feedback.retrieveSecondMarkerFeedback)
			case SecondFeedback if markerFeedback.feedback.assignment.markingWorkflow.hasThirdMarker =>
				Option(markerFeedback.feedback.retrieveThirdMarkerFeedback)
			case _ => None
		}
	}

	private def finaliseFeedback(feedbackForRelease: Seq[MarkerFeedback]) = {
		val finaliseFeedbackCommand = new FinaliseFeedbackCommand(assignment, feedbackForRelease, gradeGenerator)
		finaliseFeedbackCommand.apply()
	}
}

trait MarkingCompletedCommandPermissions extends RequiresPermissionsChecking {
	self: MarkingCompletedState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Feedback.Create, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait MarkingCompletedDescription extends Describable[Unit] {

	self: MarkingCompletedState =>

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> markerFeedback.map(_.feedback.universityId))
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
			.property("numFeedbackUpdated" -> markerFeedback.size())
	}
}

trait MarkingCompletedState {
	self : UserAware =>

	import uk.ac.warwick.tabula.JavaImports._

	val assignment: Assignment
	val module: Module

	var markerFeedback: JList[MarkerFeedback] = JArrayList()

	var noMarks: JList[MarkerFeedback] = JArrayList()
	var noFeedback: JList[MarkerFeedback] = JArrayList()
	var releasedFeedback: JList[MarkerFeedback] = JArrayList()

	var onlineMarking: Boolean = false
	var confirm: Boolean = false
	val marker = user
	val submitter: CurrentUser
}

trait SecondMarkerReleaseNotifier extends FeedbackReleasedNotifier[Unit] {
	this: MarkingCompletedState with ReleasedState with UserAware with UserLookupComponent with Logging =>
	def blankNotification = new ReleaseToMarkerNotification(2)
}

trait MarkerCompletedNotificationCompletion extends CompletesNotifications[Unit] {

	self: MarkingCompletedState with NotificationHandling =>

	def notificationsToComplete(commandResult: Unit): CompletesNotificationsResult = {
		val notificationsToComplete = markerFeedback
			.filter(_.state == MarkingState.MarkingCompleted)
			.flatMap(mf =>
				// ModeratorRejectedNotification is tied to the 2nd marker feedback
				Option(mf.feedback.secondMarkerFeedback)
					.map(notificationService.findActionRequiredNotificationsByEntityAndType[ModeratorRejectedNotification])
					.getOrElse(Seq()) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[ReleaseToMarkerNotification](mf) ++
					notificationService.findActionRequiredNotificationsByEntityAndType[ReturnToMarkerNotification](mf)
			)
		CompletesNotificationsResult(notificationsToComplete, marker)
	}
}