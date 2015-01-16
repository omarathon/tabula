package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.data.model.notifications.coursework.ReturnToMarkerNotification
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications.{FeedbackReturnedNotifier, ReleasedState}
import uk.ac.warwick.tabula.system.BindListener

object MarkingUncompletedCommand {
	def apply(module: Module, assignment: Assignment, user: User, submitter: CurrentUser) =
		new MarkingUncompletedCommand(module, assignment, user, submitter)
			with ComposableCommand[Unit]
			with MarkingUncompletedCommandPermissions
			with MarkingUncompletedDescription
			with MarkerReturnedNotifier
			with AutowiringStateServiceComponent
			with AutowiringFeedbackServiceComponent
}

abstract class MarkingUncompletedCommand(val module: Module, val assignment: Assignment, val user: User, val submitter: CurrentUser)
	extends CommandInternal[Unit]
	with Appliable[Unit]
	with SelfValidating
	with UserAware
	with MarkingUncompletedState
	with ReleasedState
	with BindListener
	with Logging {

	self: StateServiceComponent with FeedbackServiceComponent =>

	override def onBind(result: BindingResult) {
		// do not update previously released feedback
		markerFeedback = markerFeedback.asScala.filterNot(_.feedback.released)
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
		if (markerFeedback.isEmpty) errors.rejectValue("markerFeedback", "markers.finishMarking.noStudents")
	}

	def applyInternal() {
		// set the previous feedback to ReleasedForMarking
		newReleasedFeedback = markerFeedback.flatMap(getSubsequentFeedback)
		newReleasedFeedback.foreach(stateService.updateStateUnsafe(_, MarkingState.ReleasedForMarking))
		// delete the returned feedback
		markerFeedback.foreach(feedbackService.delete)
	}

	def getSubsequentFeedback(markerFeedback: MarkerFeedback) = markerFeedback.getFeedbackPosition match {
		case ThirdFeedback => Some(markerFeedback.feedback.secondMarkerFeedback)
		case SecondFeedback => Some(markerFeedback.feedback.firstMarkerFeedback)
		case FirstFeedback => None
	}

}

trait MarkingUncompletedCommandPermissions extends RequiresPermissionsChecking {
	self: MarkingUncompletedState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Feedback.Create, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait MarkingUncompletedDescription extends Describable[Unit] {

	self: MarkingUncompletedState =>

	override def describe(d: Description) {
		d.assignment(assignment)
			.property("students" -> markerFeedback.map(_.feedback.universityId))
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
			.property("numFeedbackUpdated" -> markerFeedback.size())
	}
}

trait MarkingUncompletedState {
	import uk.ac.warwick.tabula.JavaImports._

	val assignment: Assignment
	val module: Module
	val user: User
	val marker = user
	val submitter: CurrentUser

	var markerFeedback: JList[MarkerFeedback] = JArrayList()
	var confirm: Boolean = false
	var comment: String = _
}

trait MarkerReturnedNotifier extends FeedbackReturnedNotifier[Unit] {
	this: MarkingUncompletedState with ReleasedState with UserAware with Logging =>

	// take the workflow position from the first item being returned.
	val position = markerFeedback.headOption.map(_.getFeedbackPosition) match {
		case None => 3
		case Some(ThirdFeedback) => 2
		case Some(SecondFeedback) => 1
		case _ => 0
	}

	def blankNotification = new ReturnToMarkerNotification(position, comment)
}


object AdminMarkingUncompletedCommand {
	def apply(module: Module, assignment: Assignment, user: User, submitter: CurrentUser) =
		new AdminMarkingUncompletedCommand(module, assignment, user, submitter)
			with ComposableCommand[Unit]
			with MarkingUncompletedCommandPermissions
			with MarkingUncompletedDescription
			with MarkerReturnedNotifier
			with AutowiringStateServiceComponent
			with AutowiringFeedbackServiceComponent
}

// an admin version of the above command that just resets the state of the last marker feedback
abstract class AdminMarkingUncompletedCommand(module: Module, assignment: Assignment, user: User, submitter: CurrentUser)
	extends MarkingUncompletedCommand(module, assignment, user, submitter) {

	self: StateServiceComponent with FeedbackServiceComponent =>

	var students: JList[String] = JArrayList()

	override def onBind(result: BindingResult) {
		val parentFeedback = students.flatMap(feedbackService.getStudentFeedback(assignment, _))
		markerFeedback = parentFeedback.filterNot(f => f.released || f.isPlaceholder).flatMap(_.getAllCompletedMarkerFeedback.lastOption)
	}

	override def applyInternal() {
		// set the last markerfeedback to ReleasedForMarking
		newReleasedFeedback = markerFeedback
		newReleasedFeedback.foreach(stateService.updateStateUnsafe(_, MarkingState.ReleasedForMarking))
	}

}