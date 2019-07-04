package uk.ac.warwick.tabula.services.mitcircs

import enumeratum._
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.WorkflowStages.StageProgress
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmissionState.{ApprovedByChair, OutcomesRecorded}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.helpers.RequestLevelCaching
import uk.ac.warwick.tabula.{WorkflowProgress, WorkflowStage, WorkflowStageHealth, WorkflowStages}

import scala.collection.immutable

@Service
class MitCircsWorkflowProgressService extends RequestLevelCaching[Department, Seq[MitCircsWorkflowStage]] {
  final val MaxPower = 100

  // This is mostly a placeholder for if we allow any variation in the workflow in the future
  def getStagesFor(department: Department): Seq[MitCircsWorkflowStage] = cachedBy(department) {
    MitCircsWorkflowStage.values
  }

  def progress(department: Department)(submission: MitigatingCircumstancesSubmission): WorkflowProgress = {
    val allStages = getStagesFor(department)
    val progresses = allStages.map(_.progress(department)(submission))

    val workflowMap = WorkflowStages.toMap(progresses)

    // Quick exit for if we're at the end
    if (progresses.last.completed) {
      WorkflowProgress(MaxPower, progresses.last.messageCode, progresses.last.health.cssClass, None, workflowMap)
    } else {
      val stagesWithPreconditionsMet = progresses.filter(progress => workflowMap(progress.stage.toString).preconditionsMet)

      progresses.filter(_.started).lastOption match {
        case Some(lastProgress) =>
          val index = progresses.indexOf(lastProgress)

          // If the current stage is complete, the next stage requires action
          val nextProgress = if (lastProgress.completed) {
            val nextProgressCandidate = progresses(index + 1)

            if (stagesWithPreconditionsMet.contains(nextProgressCandidate)) {
              nextProgressCandidate
            } else {
              // The next stage can't start yet because its preconditions are not met.
              // Find the latest incomplete stage from earlier in the workflow whose preconditions are met.
              val earlierReadyStages = progresses.reverse
                .dropWhile(_ != nextProgressCandidate)
                .filterNot(_.completed)
                .filter(stagesWithPreconditionsMet.contains)

              earlierReadyStages.headOption.getOrElse(lastProgress)
            }
          } else {
            lastProgress
          }

          val percentage = ((index + 1) * MaxPower) / allStages.size
          WorkflowProgress(percentage, lastProgress.messageCode, lastProgress.health.cssClass, Some(nextProgress.stage), workflowMap)
        case None =>
          WorkflowProgress(0, progresses.head.messageCode, progresses.head.health.cssClass, None, workflowMap)
      }
    }
  }
}

sealed abstract class MitCircsWorkflowStage extends WorkflowStage with EnumEntry {
  def progress(department: Department)(submission: MitigatingCircumstancesSubmission): WorkflowStages.StageProgress
  override val actionCode: String = s"workflow.mitCircs.$entryName.action"
}

object MitCircsWorkflowStage extends Enum[MitCircsWorkflowStage] {
  // Student submits form
  case object Submission extends MitCircsWorkflowStage {
    override def progress(department: Department)(submission: MitigatingCircumstancesSubmission): WorkflowStages.StageProgress =
      submission.state match {
        case MitigatingCircumstancesSubmissionState.Withdrawn =>
          StageProgress(
            stage = Submission,
            started = true,
            messageCode = "workflow.mitCircs.Submission.withdrawn",
            completed = true,
            health = WorkflowStageHealth.Danger,
          )

        case MitigatingCircumstancesSubmissionState.Draft =>
          StageProgress(
            stage = Submission,
            started = true,
            messageCode = "workflow.mitCircs.Submission.draft",
          )

        case MitigatingCircumstancesSubmissionState.CreatedOnBehalfOfStudent =>
          StageProgress(
            stage = Submission,
            started = true,
            messageCode = "workflow.mitCircs.Submission.createdOnBehalfOfStudent",
            health = WorkflowStageHealth.Warning,
          )

        case _ =>
          StageProgress(
            stage = Submission,
            started = true,
            messageCode = "workflow.mitCircs.Submission.submitted",
            completed = true,
          )
      }
  }

  // MCO reviews, messages student
  case object InitialAssessment extends MitCircsWorkflowStage {
    override def progress(department: Department)(submission: MitigatingCircumstancesSubmission): WorkflowStages.StageProgress =
      submission.state match {
        case MitigatingCircumstancesSubmissionState.Submitted | MitigatingCircumstancesSubmissionState.ReadyForPanel | MitigatingCircumstancesSubmissionState.OutcomesRecorded | MitigatingCircumstancesSubmissionState.ApprovedByChair =>
          if (submission.messages.isEmpty) {
            if (submission.state == MitigatingCircumstancesSubmissionState.Submitted) {
              StageProgress(
                stage = InitialAssessment,
                started = false,
                messageCode = "workflow.mitCircs.InitialAssessment.notAssessed",
              )
            } else {
              StageProgress(
                stage = InitialAssessment,
                started = false,
                messageCode = "workflow.mitCircs.InitialAssessment.skipped",
                skipped = true,
              )
            }
          } else if (submission.messages.last.studentSent) {
            StageProgress(
              stage = InitialAssessment,
              started = true,
              messageCode = "workflow.mitCircs.InitialAssessment.messageReceived",
              health = WorkflowStageHealth.Warning,
            )
          } else {
            StageProgress(
              stage = InitialAssessment,
              started = true,
              messageCode = "workflow.mitCircs.InitialAssessment.messageSent",
              completed = true,
            )
          }

        case _ =>
          StageProgress(
            stage = InitialAssessment,
            started = false,
            messageCode = "workflow.mitCircs.InitialAssessment.notAssessed",
          )
      }

    override val preconditions: Seq[Seq[WorkflowStage]] = Seq(Seq(Submission))
  }

  // MCO confirms that submission contains sufficient information to be reviewed
  case object ReadyForPanel extends MitCircsWorkflowStage {
    override def progress(department: Department)(submission: MitigatingCircumstancesSubmission): WorkflowStages.StageProgress = submission.state match {
      case MitigatingCircumstancesSubmissionState.OutcomesRecorded if submission.isAcute =>
        StageProgress(
          stage = ReadyForPanel,
          started = true,
          messageCode = "workflow.mitCircs.ReadyForPanel.skipped",
          skipped = true,
        )

      case MitigatingCircumstancesSubmissionState.ReadyForPanel | MitigatingCircumstancesSubmissionState.OutcomesRecorded | MitigatingCircumstancesSubmissionState.ApprovedByChair =>
        StageProgress(
          stage = ReadyForPanel,
          started = true,
          messageCode = "workflow.mitCircs.ReadyForPanel.readyForReview",
          completed = true,
        )

      case MitigatingCircumstancesSubmissionState.Submitted if submission.hasSensitiveEvidence  => if (submission.sensitiveEvidenceSeenBy == null) {
        StageProgress(
          stage = ReadyForPanel,
          started = true,
          messageCode = "workflow.mitCircs.ReadyForPanel.sensitiveEvidenceNeedsReview"
        )
      } else {
        StageProgress(
          stage = ReadyForPanel,
          started = true,
          preconditionsMet = true,
          messageCode = "workflow.mitCircs.ReadyForPanel.sensitiveEvidenceReviewed"
        )
      }

      case _ =>
        StageProgress(
          stage = ReadyForPanel,
          started = false,
          messageCode = "workflow.mitCircs.ReadyForPanel.notReady",
        )
    }

    override val preconditions: Seq[Seq[WorkflowStage]] = Seq(Seq(Submission))
  }

  // MCO presents cases to MC panel
  case object SelectedForPanel extends MitCircsWorkflowStage {
    override def progress(department: Department)(submission: MitigatingCircumstancesSubmission): WorkflowStages.StageProgress = submission.state match {
      case MitigatingCircumstancesSubmissionState.OutcomesRecorded if submission.isAcute =>
        StageProgress(
          stage = SelectedForPanel,
          started = true,
          messageCode = "workflow.mitCircs.SelectedForPanel.skipped",
          skipped = true,
        )

      case _ =>
        if (submission.panel.nonEmpty) {
          StageProgress(
            stage = SelectedForPanel,
            started = true,
            messageCode = "workflow.mitCircs.SelectedForPanel.selected",
            completed = true
          )
        } else {
          StageProgress(
            stage = SelectedForPanel,
            started = false,
            messageCode = "workflow.mitCircs.SelectedForPanel.notSelected",
          )
        }
    }

    override val preconditions: Seq[Seq[WorkflowStage]] = Seq(Seq(ReadyForPanel))
  }

  // Panel agrees reject / mild / moderate / severe outcome and duration
  case object Outcomes extends MitCircsWorkflowStage {
    override def progress(department: Department)(submission: MitigatingCircumstancesSubmission): WorkflowStages.StageProgress = submission.state match {
      case OutcomesRecorded | ApprovedByChair =>
        StageProgress(
          stage = Outcomes,
          started = true,
          messageCode = "workflow.mitCircs.Outcomes.recorded",
          completed = true
        )

      case _ =>
        StageProgress(
          stage = Outcomes,
          started = false,
          messageCode = "workflow.mitCircs.Outcomes.notRecorded",
        )
    }


    override val preconditions: Seq[Seq[WorkflowStage]] = Seq(Seq(Submission), Seq(Submission, SelectedForPanel))
  }

  case object Approved extends MitCircsWorkflowStage {
    override def progress(department: Department)(submission: MitigatingCircumstancesSubmission): WorkflowStages.StageProgress = submission.state match {

      case MitigatingCircumstancesSubmissionState.OutcomesRecorded if submission.isAcute =>
        StageProgress(
          stage = Approved,
          started = true,
          messageCode = "workflow.mitCircs.Approved.skipped",
          skipped = true,
        )

      case ApprovedByChair =>
        StageProgress(
          stage = Approved,
          started = true,
          messageCode = "workflow.mitCircs.Approved.approved",
          completed = true
        )

      case _ =>
        StageProgress(
          stage = Approved,
          started = false,
          messageCode = "workflow.mitCircs.Approved.notApproved",
        )
    }


    override val preconditions: Seq[Seq[WorkflowStage]] = Seq(Seq(Outcomes))
  }

  override val values: immutable.IndexedSeq[MitCircsWorkflowStage] = findValues
}

trait MitCircsWorkflowProgressServiceComponent {
  def workflowProgressService: MitCircsWorkflowProgressService
}

trait AutowiringMitCircsWorkflowProgressServiceComponent extends MitCircsWorkflowProgressServiceComponent {
  var workflowProgressService: MitCircsWorkflowProgressService = Wire[MitCircsWorkflowProgressService]
}
