package uk.ac.warwick.tabula.coursework.commands.assignments


import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{FeedbackServiceComponent, AutowiringFeedbackServiceComponent, AutowiringUserLookupComponent, UserLookupComponent, AutowiringStateServiceComponent, StateServiceComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications.{ReleasedState, FeedbackReleasedNotifier}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.notifications.ReleaseToMarkerNotification
import scala.collection.mutable

object MarkingCompletedCommand {
	def apply(module: Module, assignment: Assignment, user: User, firstMarker:Boolean) =
		new MarkingCompletedCommand(module, assignment, user, firstMarker)
			with ComposableCommand[Unit]
			with MarkingCompletedCommandPermissions
			with MarkingCompletedDescription
			with SecondMarkerReleaseNotifier
			with AutowiringUserLookupComponent
			with AutowiringStateServiceComponent
			with AutowiringFeedbackServiceComponent
}

abstract class MarkingCompletedCommand(val module: Module, val assignment: Assignment, val user: User, val firstMarker:Boolean)
	extends CommandInternal[Unit] with Appliable[Unit] with SelfValidating with UserAware with MarkingCompletedState
	with ReleasedState {

	this: StateServiceComponent with FeedbackServiceComponent =>

	def onBind() {
		markerFeedbacks = students.flatMap(assignment.getMarkerFeedbackForCurrentPosition(_, user))
	}

	def applyInternal() {
		// do not update previously released feedback
		val feedbackForRelease = markerFeedbacks -- releasedFeedback

		feedbackForRelease.foreach(stateService.updateState(_, MarkingState.MarkingCompleted))

		nextMarkerFeedback(feedbackForRelease)

	}

	private def nextMarkerFeedback(feedbackForRelease: mutable.Buffer[MarkerFeedback]){

		def finaliseFeedback(){
			val finaliseFeedbackCommand = new FinaliseFeedbackCommand(assignment, feedbackForRelease)
			finaliseFeedbackCommand.apply()
		}

		newReleasedFeedback = feedbackForRelease.map{ mf =>
			val parentFeedback = mf.feedback
			val nextMarkerFeedback = {
				if (mf.state == MarkingState.MarkingCompleted && mf.getFeedbackPosition.get == FirstFeedback)	{
					parentFeedback.retrieveSecondMarkerFeedback
				}
				else {
					parentFeedback.retrieveThirdMarkerFeedback

				}
			}
			if (mf.getFeedbackPosition.get != ThirdFeedback)	{
				stateService.updateState(nextMarkerFeedback, MarkingState.ReleasedForMarking)
			}
			feedbackService.save(nextMarkerFeedback)


			// if we're completing the last piece of marker feedback, then finalise the feedback
			if (mf.getFeedbackPosition.get == nextMarkerFeedback.getFeedbackPosition.get) finaliseFeedback()

			nextMarkerFeedback
		}

	}

	def preSubmitValidation() {
		noMarks = markerFeedbacks.filter(!_.hasMark)
		noFeedback = markerFeedbacks.filter(!_.hasFeedback)
		releasedFeedback = markerFeedbacks.filter(_.state == MarkingState.MarkingCompleted)
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
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
			.property("numFeedbackUpdated" -> markerFeedbacks.size())
	}
}

trait MarkingCompletedState {

	import uk.ac.warwick.tabula.JavaImports._

	val assignment: Assignment
	val module: Module

	var students: JList[String] = JArrayList()
	var markerFeedbacks: JList[MarkerFeedback] = JArrayList()

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