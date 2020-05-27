package uk.ac.warwick.tabula.services.marks

import enumeratum.{Enum, EnumEntry}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.WorkflowStages.StageProgress
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.{AssessmentComponentInfo, StudentMarkRecord}
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.{AcademicYear, WorkflowProgress, WorkflowStage, WorkflowStageHealth, WorkflowStages}

@Service
class MarksWorkflowProgressService {
  final val MaxPower = 100

  def componentStages(
    assessmentComponent: AssessmentComponent,
    upstreamAssessmentGroup: UpstreamAssessmentGroup,
  ): Seq[ComponentMarkWorkflowStage] = ComponentMarkWorkflowStage.values.filter(_.applies(assessmentComponent, upstreamAssessmentGroup))

  def componentProgress(
    assessmentComponent: AssessmentComponent,
    upstreamAssessmentGroup: UpstreamAssessmentGroup,
    students: Seq[StudentMarkRecord]
  ): WorkflowProgress = {
    ???
  }

  def moduleOccurrenceStages(): Seq[ModuleOccurrenceMarkWorkflowStage] = ???

  def moduleOccurrenceProgress(components: Seq[AssessmentComponentInfo]): WorkflowProgress = {
    ???
  }
}

sealed abstract class ModuleOccurrenceMarkWorkflowStage extends WorkflowStage with EnumEntry {
  def progress(components: Seq[AssessmentComponentInfo]): WorkflowStages.StageProgress
  override val actionCode: String = s"workflow.marks.moduleOccurrence.$entryName.action"
}

object ModuleOccurrenceMarkWorkflowStage extends Enum[ModuleOccurrenceMarkWorkflowStage] {
  // Upload component marks or MMA
  // Calculate module marks - WARN if out of sync, inProgress if needs syncing to SITS
  // Confirm module marks
  // Process module marks

  override val values: IndexedSeq[ModuleOccurrenceMarkWorkflowStage] = findValues
}

sealed abstract class ComponentMarkWorkflowStage extends WorkflowStage with EnumEntry {
  def applies(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup): Boolean = true
  def progress(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, students: Seq[StudentMarkRecord]): WorkflowStages.StageProgress
  override val actionCode: String = s"workflow.marks.component.$entryName.action"
}

object ComponentMarkWorkflowStage extends Enum[ComponentMarkWorkflowStage] {
  // Provide deadline (19/20 assessments only)
  // Upload unconfirmed actual component marks (MMA skips this) - WARN if out of sync, inProgress if needs syncing to SITS
  // Confirm actual component marks (happens when module marks are confirmed)
  // Process component marks (happens when module marks are processed)

  case object SetDeadline extends ComponentMarkWorkflowStage {
    override def applies(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup): Boolean = {
      upstreamAssessmentGroup.academicYear == AcademicYear.starting(2019) // Only need deadlines for 19/20, and only for UG modules
    }

    override def progress(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, students: Seq[StudentMarkRecord]): WorkflowStages.StageProgress =
      if (upstreamAssessmentGroup.deadline.isEmpty) {
        StageProgress(
          stage = SetDeadline,
          started = false,
          messageCode = "workflow.marks.component.SetDeadline.notProvided",
          health = WorkflowStageHealth.Danger,
        )
      } else {

      }
  }

  override val values: IndexedSeq[ComponentMarkWorkflowStage] = findValues
}

trait MarksWorkflowProgressServiceComponent {
  def workflowProgressService: MarksWorkflowProgressService
}

trait AutowiringMarksWorkflowProgressServiceComponent extends MarksWorkflowProgressServiceComponent {
  var workflowProgressService: MarksWorkflowProgressService = Wire[MarksWorkflowProgressService]
}



