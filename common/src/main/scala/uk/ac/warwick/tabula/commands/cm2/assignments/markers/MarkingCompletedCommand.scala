package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.markingworkflow.{FinalStage, MarkingWorkflowStage}
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, Feedback, MarkerFeedback}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.util.Try


object MarkingCompletedCommand {
	def apply(assignment: Assignment, marker: User, submitter: CurrentUser, stage: MarkingWorkflowStage) =
		new MarkingCompletedCommandInternal(assignment, marker, submitter, stage)
			with ComposableCommand[Seq[AssignmentFeedback]]
			with MarkingCompletedValidation
			with MarkingCompletedPermissions
			with MarkingCompletedDescription
			with AutowiringCM2MarkingWorkflowServiceComponent
			with FinaliseFeedbackComponentImpl
}

class MarkingCompletedCommandInternal(val assignment: Assignment, val marker: User, val submitter: CurrentUser, val stage: MarkingWorkflowStage)
	extends CommandInternal[Seq[AssignmentFeedback]] with MarkingCompletedState with MarkingCompletedValidation {

	this: CM2MarkingWorkflowServiceComponent with FinaliseFeedbackComponent =>

	def applyInternal(): Seq[AssignmentFeedback] = {
		val feedback = feedbackForRelease.map(mf => HibernateHelpers.initialiseAndUnproxy(mf.feedback)).collect{ case f: AssignmentFeedback => f }

		val completed = cm2MarkingWorkflowService.progressFeedback(stage, feedback)
		// finalise any feedback that has finished the workflow
		val toFinalise = {
			val feedbackAtFinalStage = completed.filter(f => f.outstandingStages.asScala.collect{case s: FinalStage => s}.nonEmpty).map(_.id)
			feedbackForRelease.filter(mf => feedbackAtFinalStage.contains(mf.feedback.id))
		}
		finaliseFeedback(assignment, toFinalise)
		completed
	}
}

trait MarkingCompletedPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: MarkingCompletedState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait MarkingCompletedValidation extends SelfValidating {
	self: MarkingCompletedState =>
	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
		if (markerFeedback.isEmpty) errors.rejectValue("markerFeedback", "markerFeedback.finishMarking.noStudents")
	}
}

trait MarkingCompletedDescription extends Describable[Seq[AssignmentFeedback]] {

	self: MarkingCompletedState =>

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> feedbackForRelease.map(_.feedback.usercode))
	}

	override def describeResult(d: Description, result: Seq[AssignmentFeedback]){
		d.assignment(assignment)
			.property("numFeedbackUpdated" -> result.length)
	}
}

trait MarkingCompletedState extends CanProxy {

	import uk.ac.warwick.tabula.JavaImports._

	var markerFeedback: JList[MarkerFeedback] = JArrayList()
	var confirm: Boolean = false

	val assignment: Assignment
	val marker: User
	val submitter: CurrentUser
	val stage: MarkingWorkflowStage

	// Pre-submit validation
	def noMarks: Seq[MarkerFeedback] = markerFeedback.asScala.filter(!_.hasMark)
	def noFeedback: Seq[MarkerFeedback] = markerFeedback.asScala.filter(!_.hasFeedback)
	def noContent: Seq[MarkerFeedback] = markerFeedback.asScala.filter(!_.hasContent) // should be empty
	def releasedFeedback: Seq[MarkerFeedback] = markerFeedback.asScala.filter(mf => {
		val currentStageIndex = mf.feedback.outstandingStages.asScala.map(_.order).headOption.getOrElse(0)
		mf.stage.order < currentStageIndex
	})

	// do not update previously released feedback or feedback belonging to other markers
	lazy val feedbackForRelease: Seq[MarkerFeedback] = markerFeedback.asScala.filter(_.marker == marker) -- releasedFeedback
}