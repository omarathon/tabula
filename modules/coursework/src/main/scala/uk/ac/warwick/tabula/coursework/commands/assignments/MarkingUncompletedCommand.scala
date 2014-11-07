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
		completedMarkerFeedback = students.asScala.flatMap(assignment.getLatestCompletedMarkerFeedback(_, user)).asJava
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
		if (completedMarkerFeedback.isEmpty) errors.rejectValue("students", "markers.finishMarking.noStudents")
	}

	def applyInternal() {
		// do not update previously released feedback
		val feedbackForRelease = completedMarkerFeedback.asScala.filterNot(_.feedback.released).flatMap(getMarkerFeedbackForRelease)

		feedbackForRelease.foreach(stateService.updateStateUnsafe(_, MarkingState.ReleasedForMarking))
	}

	private def getMarkerFeedbackForRelease(markerFeedback: MarkerFeedback): Seq[MarkerFeedback] = {
		val subsequentFeedbacks = markerFeedback.getFeedbackPosition match {
			case ThirdFeedback => Seq()
			case SecondFeedback => Seq(markerFeedback.feedback.thirdMarkerFeedback)
			case FirstFeedback => Seq(markerFeedback.feedback.secondMarkerFeedback, markerFeedback.feedback.thirdMarkerFeedback)
		}
		val subsequentFeedbacksNotNull = subsequentFeedbacks.filterNot(_ == null)
		Seq(markerFeedback) ++ subsequentFeedbacksNotNull
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
			.property("numFeedbackUpdated" -> completedMarkerFeedback.size())
	}
}

trait MarkingUncompletedState {
	import uk.ac.warwick.tabula.JavaImports._

	val assignment: Assignment
	val module: Module
	val user: User

	var students: JList[String] = JArrayList()
	var completedMarkerFeedback: JList[MarkerFeedback] = JArrayList()

	var onlineMarking: Boolean = false
	var confirm: Boolean = false
}