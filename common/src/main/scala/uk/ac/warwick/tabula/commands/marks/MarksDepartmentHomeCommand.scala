package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.AssessmentComponentInfo
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringSecurityServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, WorkflowStage, WorkflowStages}

import scala.collection.immutable.ListMap

object MarksDepartmentHomeCommand {
  case class MarksWorkflowProgress(percentage: Int, t: String, messageCode: String)

  case class ModuleOccurrence(
    moduleCode: String,
    module: Module,
    occurrence: String,

    // Progress
    progress: MarksWorkflowProgress,
    nextStage: Option[WorkflowStage],
    stages: ListMap[String, WorkflowStages.StageProgress],

    // Assessment components grouped by assessment group
    assessmentComponents: Seq[(String, Seq[AssessmentComponentInfo])],
  )

  type Result = Seq[ModuleOccurrence]
  type Command = Appliable[Result]

  def apply(department: Department, academicYear: AcademicYear, currentUser: CurrentUser): Command =
    new MarksDepartmentHomeCommandInternal(department, academicYear, currentUser)
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with ComposableCommand[Result]
      with ListAssessmentComponentsModulesWithPermission
      with ListAssessmentComponentsPermissions
      with Unaudited with ReadOnly
}

abstract class MarksDepartmentHomeCommandInternal(val department: Department, val academicYear: AcademicYear, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with ListAssessmentComponentsState
    with ListAssessmentComponentsForModulesWithPermission {
  self: AssessmentComponentMarksServiceComponent
    with AssessmentMembershipServiceComponent
    with ListAssessmentComponentsModulesWithPermission =>

  override def applyInternal(): Result = {
    assessmentComponentInfos
      .groupBy { info => (info.assessmentComponent.moduleCode, info.upstreamAssessmentGroup.occurrence) }
      .map { case ((moduleCode, occurrence), infos) =>
        ModuleOccurrence(
          moduleCode = moduleCode,
          module = infos.head.assessmentComponent.module,
          occurrence = occurrence,
          assessmentComponents =
            infos.groupBy(_.assessmentComponent.assessmentGroup)
              .toSeq
              .sortBy { case (assessmentGroup, _) => assessmentGroup }
        )
      }
      .toSeq.sortBy { mo => (mo.moduleCode, mo.occurrence) }
  }
}
