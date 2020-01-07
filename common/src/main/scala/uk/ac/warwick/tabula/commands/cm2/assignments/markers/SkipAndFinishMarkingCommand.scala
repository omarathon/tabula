package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringCM2MarkingWorkflowServiceComponent, CM2MarkingWorkflowServiceComponent}
import uk.ac.warwick.userlookup.User


object SkipAndFinishMarkingCommand {
  def apply(assignment: Assignment, marker: User, submitter: CurrentUser, stagePosition: Int) =
    new SkipAndFinishMarkingCommandInternal(assignment, marker, submitter, stagePosition)
      with ComposableCommand[Seq[Feedback]]
      with WorkflowProgressValidation
      with WorkflowProgressPermissions
      with SkipAndFinishMarkingDescription
      with AutowiringCM2MarkingWorkflowServiceComponent
      with FinaliseFeedbackComponentImpl
      with PopulateMarkerFeedbackComponentImpl
      with FinaliseFeedbackNotifier
}

class SkipAndFinishMarkingCommandInternal(val assignment: Assignment, val marker: User, val submitter: CurrentUser, val stagePosition: Int)
  extends CommandInternal[Seq[Feedback]] with WorkflowProgressState with WorkflowProgressValidation {

  self: CM2MarkingWorkflowServiceComponent with FinaliseFeedbackComponent with PopulateMarkerFeedbackComponent =>

  def applyInternal(): Seq[Feedback] = transactional() {

    val feedbackForSkippingByStage = cm2MarkingWorkflowService.getAllFeedbackForMarker(assignment, marker)
      .view.filterKeys(_.order == stagePosition)
      .mapValues(_.filter(feedbackForRelease.contains))

    val finalisedFeedback = for {
      (stage, mf) <- feedbackForSkippingByStage
      previousStage <- stage.previousStages.headOption
    } yield {
      val f = mf.map(mf => HibernateHelpers.initialiseAndUnproxy(mf.feedback)).filter(_.outstandingStages.contains(stage))
      cm2MarkingWorkflowService.finish(previousStage, f)
    }

    finalisedFeedback.flatten.collect { case f: Feedback => f }.toSeq
  }
}

trait SkipAndFinishMarkingDescription extends WorkflowProgressDescription {
  self: WorkflowProgressState =>
  override lazy val eventName: String = "SkipAndFinishMarking"
}