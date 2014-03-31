package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.services.{FeedbackServiceComponent, AutowiringFeedbackServiceComponent, AutowiringUserLookupComponent, UserLookupComponent, AutowiringStateServiceComponent, StateServiceComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications.{ReleasedState, FeedbackReleasedNotifier}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.notifications.ReleaseToMarkerNotification
import scala.collection.mutable
import uk.ac.warwick.tabula.system.BindListener

object MarkingCompletedCommand {
	def apply(module: Module, assignment: Assignment, user: User) =
		new MarkingCompletedCommand(module, assignment, user)
			with ComposableCommand[Unit]
			with MarkingCompletedCommandPermissions
			with MarkingCompletedDescription
			with SecondMarkerReleaseNotifier
			with AutowiringUserLookupComponent
			with AutowiringStateServiceComponent
			with AutowiringFeedbackServiceComponent
}

abstract class MarkingCompletedCommand(val module: Module, val assignment: Assignment, val user: User)
	extends CommandInternal[Unit] with Appliable[Unit] with SelfValidating with UserAware with MarkingCompletedState with ReleasedState with BindListener {

	self: StateServiceComponent with FeedbackServiceComponent =>

	override def onBind(result: BindingResult) {
		pendingMarkerFeedbacks = students.flatMap(assignment.getMarkerFeedbackForCurrentPosition(_, user)).filter(null != _)
	}

	def preSubmitValidation() {
		noMarks = pendingMarkerFeedbacks.filter(!_.hasMark)
		noFeedback = pendingMarkerFeedbacks.filter(!_.hasFeedback)
		releasedFeedback = pendingMarkerFeedbacks.filter(_.state == MarkingState.MarkingCompleted)
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
		if (pendingMarkerFeedbacks.isEmpty) errors.rejectValue("students", "markers.finishMarking.noStudents")
	}

	def applyInternal() {
		// do not update previously released feedback
		val feedbackForRelease = pendingMarkerFeedbacks -- releasedFeedback

		feedbackForRelease.foreach(stateService.updateState(_, MarkingState.MarkingCompleted))

		releaseNextMarkerFeedbackOrFinalise(feedbackForRelease)
	}

	private def releaseNextMarkerFeedbackOrFinalise(feedbackForRelease: mutable.Buffer[MarkerFeedback]) {
		newReleasedFeedback = feedbackForRelease.filter(nextMarkerFeedback(_).isDefined).map{ nextMarkerFeedback =>
			stateService.updateState(nextMarkerFeedback, MarkingState.ReleasedForMarking)
			feedbackService.save(nextMarkerFeedback)
			nextMarkerFeedback
		}

		finaliseFeedback(feedbackForRelease.filter(!nextMarkerFeedback(_).isDefined))
	}

	private def nextMarkerFeedback(markerFeedback: MarkerFeedback): Option[MarkerFeedback] = {
		markerFeedback.getFeedbackPosition match {
			case Some(FirstFeedback) if markerFeedback.feedback.assignment.markingWorkflow.hasSecondMarker =>
				Option(markerFeedback.feedback.retrieveSecondMarkerFeedback)
			case Some(SecondFeedback) if markerFeedback.feedback.assignment.markingWorkflow.hasThirdMarker =>
				Option(markerFeedback.feedback.retrieveThirdMarkerFeedback)
			case _ => None
		}
	}

	private def finaliseFeedback(feedbackForRelease: Seq[MarkerFeedback]) = {
		val finaliseFeedbackCommand = new FinaliseFeedbackCommand(assignment, feedbackForRelease)
		finaliseFeedbackCommand.apply()
	}
}

trait MarkingCompletedCommandPermissions extends RequiresPermissionsChecking {
	self: MarkingCompletedState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Feedback.Create, assignment)
	}
}

trait MarkingCompletedDescription extends Describable[Unit] {

	self: MarkingCompletedState =>

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> students)
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
			.property("numFeedbackUpdated" -> pendingMarkerFeedbacks.size())
	}
}

trait MarkingCompletedState {

	import uk.ac.warwick.tabula.JavaImports._

	val assignment: Assignment
	val module: Module

	var students: JList[String] = JArrayList()
	var pendingMarkerFeedbacks: JList[MarkerFeedback] = JArrayList()

	var noMarks: JList[MarkerFeedback] = JArrayList()
	var noFeedback: JList[MarkerFeedback] = JArrayList()
	var releasedFeedback: JList[MarkerFeedback] = JArrayList()

	var onlineMarking: Boolean = false
	var confirm: Boolean = false
}

trait SecondMarkerReleaseNotifier extends FeedbackReleasedNotifier[Unit] {
	this: MarkingCompletedState with ReleasedState with UserAware with UserLookupComponent with Logging =>
	def blankNotification = new ReleaseToMarkerNotification(2)
}