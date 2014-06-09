package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.services.{FeedbackServiceComponent, AutowiringFeedbackServiceComponent, AutowiringStateServiceComponent, StateServiceComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications.ReleasedState
import uk.ac.warwick.tabula.system.BindListener

object MarkingUncompletedCommand {
	def apply(module: Module, assignment: Assignment, user: User) =
		new MarkingUncompletedCommand(module, assignment, user)
			with ComposableCommand[Unit]
			with MarkingUncompletedCommandPermissions
			with MarkingUncompletedDescription
			with AutowiringStateServiceComponent
			with AutowiringFeedbackServiceComponent
}

abstract class MarkingUncompletedCommand(val module: Module, val assignment: Assignment, val user: User)
	extends CommandInternal[Unit] with Appliable[Unit] with SelfValidating with UserAware with MarkingUncompletedState with ReleasedState with BindListener {

	self: StateServiceComponent with FeedbackServiceComponent =>

	override def onBind(result: BindingResult) {
		releasedFeedback = students.asScala.flatMap(assignment.getMarkerFeedbackForPreviousPosition(_, user)).filter { mf => mf != null && mf.state == MarkingState.MarkingCompleted }.asJava
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
		if (releasedFeedback.isEmpty) errors.rejectValue("students", "markers.finishMarking.noStudents")
	}

	def applyInternal() {
		// do not update previously released feedback
		val feedbackForRelease = releasedFeedback.asScala.filterNot { _.feedback.released }

		feedbackForRelease.foreach(stateService.updateStateUnsafe(_, MarkingState.ReleasedForMarking))
	}
}

trait MarkingUncompletedCommandPermissions extends RequiresPermissionsChecking {
	self: MarkingUncompletedState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Feedback.Create, assignment)
	}
}

trait MarkingUncompletedDescription extends Describable[Unit] {

	self: MarkingUncompletedState =>

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> students)
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
			.property("numFeedbackUpdated" -> releasedFeedback.size())
	}
}

trait MarkingUncompletedState {
	import uk.ac.warwick.tabula.JavaImports._

	val assignment: Assignment
	val module: Module

	var students: JList[String] = JArrayList()
	var releasedFeedback: JList[MarkerFeedback] = JArrayList()

	var onlineMarking: Boolean = false
	var confirm: Boolean = false
}