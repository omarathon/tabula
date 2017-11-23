package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent}
import uk.ac.warwick.userlookup.User


object FinishMarkingCommand {
	def apply(assignment: Assignment, marker: User, submitter: CurrentUser, stagePosition: Int) =
		new FinishMarkingCommandInternal(assignment, marker, submitter, stagePosition)
			with ComposableCommand[Seq[AssignmentFeedback]]
			with WorkflowProgressValidation
			with WorkflowProgressPermissions
			with FinishMarkingDescription
			with AutowiringCM2MarkingWorkflowServiceComponent
			with FinaliseFeedbackComponentImpl
			with PopulateMarkerFeedbackComponentImpl
			with FinaliseFeedbackNotifier
}

class FinishMarkingCommandInternal(val assignment: Assignment, val marker: User, val submitter: CurrentUser, val stagePosition: Int)
	extends CommandInternal[Seq[AssignmentFeedback]] with WorkflowProgressState with WorkflowProgressValidation {

	self: CM2MarkingWorkflowServiceComponent with FinaliseFeedbackComponent with PopulateMarkerFeedbackComponent =>

	def applyInternal(): Seq[AssignmentFeedback] = transactional() {

		val feedbackForReleaseByStage = cm2MarkingWorkflowService.getAllFeedbackForMarker(assignment, marker)
			.filterKeys(_.order == stagePosition)
			.mapValues(_.filter(feedbackForRelease.contains))

		feedbackForReleaseByStage.flatMap{case (stage, mf) =>
			val f = mf.map(mf => HibernateHelpers.initialiseAndUnproxy(mf.feedback)).filter(_.outstandingStages.contains(stage))
			cm2MarkingWorkflowService.finish(stage, f)
		}.collect{ case f: AssignmentFeedback => f }.toSeq

	}
}

trait FinishMarkingDescription extends WorkflowProgressDescription {
	self: WorkflowProgressState =>
	override lazy val eventName: String = "FinishMarking"
}