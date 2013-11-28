package uk.ac.warwick.tabula.coursework.commands.assignments


import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent, AutowiringStateServiceComponent, StateServiceComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications.{ReleasedState, FeedbackReleasedNotifier}
import uk.ac.warwick.tabula.helpers.Logging

object MarkingCompletedCommand {
	def apply(module: Module, assignment: Assignment, user: User, firstMarker:Boolean) =
		new MarkingCompletedCommand(module, assignment, user, firstMarker)
			with ComposableCommand[Unit]
			with MarkingCompletedCommandPermissions
			with MarkingCompletedDescription
			with SecondMarkerReleaseNotifier
			with AutowiringUserLookupComponent
			with AutowiringStateServiceComponent
}

abstract class MarkingCompletedCommand(val module: Module, val assignment: Assignment, val user: User, val firstMarker:Boolean)
	extends CommandInternal[Unit] with Appliable[Unit] with SelfValidating with UserAware with MarkingCompletedState
	with ReleasedState {

	this: StateServiceComponent =>

	def onBind() {
		markerFeedbacks = students.flatMap(assignment.getMarkerFeedback(_, user))
	}

	def applyInternal() {
		// do not update previously released feedback
		val feedbackForRelease = markerFeedbacks -- releasedFeedback
		feedbackForRelease.foreach(stateService.updateState(_, MarkingState.MarkingCompleted))

		def finaliseFeedback(){
			val finaliseFeedbackCommand = new FinaliseFeedbackCommand(assignment, feedbackForRelease)
			finaliseFeedbackCommand.apply()
		}

		def createSecondMarkerFeedback(){
			newReleasedFeedback = feedbackForRelease.map{ mf =>
				val parentFeedback = mf.feedback
				val secondMarkerFeedback = parentFeedback.retrieveSecondMarkerFeedback
				stateService.updateState(secondMarkerFeedback, MarkingState.ReleasedForMarking)
				secondMarkerFeedback
			}
		}

		if (assignment.markingWorkflow.hasSecondMarker && firstMarker)
			createSecondMarkerFeedback()
		else
			finaliseFeedback()

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
	def isFirstMarker = false
}