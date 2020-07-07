package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.AssessmentComponentInfo
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.marks._
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

  case class StudentModuleMarkRecord(
    sprCode: String,
    mark: Option[Int],
    grade: Option[String],
    result: Option[ModuleResult],
    needsWritingToSits: Boolean,
    outOfSync: Boolean,
    markState: Option[MarkState],
    agreed: Boolean,
    history: Seq[RecordedModuleMark], // Most recent first
    moduleRegistration: ModuleRegistration,
    requiresResit: Boolean,
  )
  object StudentModuleMarkRecord {
    def apply(moduleRegistration: ModuleRegistration, recordedModuleRegistration: Option[RecordedModuleRegistration], requiresResit: Boolean): StudentModuleMarkRecord = {
      val isAgreedSITS = recordedModuleRegistration.forall(!_.needsWritingToSits) && (moduleRegistration.agreedMark.nonEmpty || moduleRegistration.agreedGrade.nonEmpty)

      StudentModuleMarkRecord(
        sprCode = moduleRegistration.sprCode,

        // These are needlessly verbose but thought better to be explicit on the order
        mark = recordedModuleRegistration match {
          case Some(marks) if marks.needsWritingToSits => marks.latestMark
          case _ if isAgreedSITS => moduleRegistration.agreedMark
          case Some(marks) => marks.latestMark
          case _ => moduleRegistration.firstDefinedMark
        },
        grade = recordedModuleRegistration match {
          case Some(marks) if marks.needsWritingToSits => marks.latestGrade
          case _ if isAgreedSITS => moduleRegistration.agreedGrade
          case Some(marks) => marks.latestGrade
          case _ => moduleRegistration.firstDefinedGrade
        },
        result = recordedModuleRegistration match {
          case Some(marks) if marks.needsWritingToSits => marks.latestResult
          case _ if isAgreedSITS => Option(moduleRegistration.moduleResult)
          case Some(marks) => marks.latestResult
          case _ => Option(moduleRegistration.moduleResult)
        },
        needsWritingToSits = recordedModuleRegistration.exists(_.needsWritingToSits),
        outOfSync = recordedModuleRegistration.exists(!_.needsWritingToSits) && (
          recordedModuleRegistration.flatMap(_.latestMark).exists(m => !moduleRegistration.firstDefinedMark.contains(m)) ||
          recordedModuleRegistration.flatMap(_.latestGrade).exists(g => !moduleRegistration.firstDefinedGrade.contains(g)) ||
          recordedModuleRegistration.flatMap(_.latestResult).exists(r => moduleRegistration.moduleResult != r)
        ),
        markState = recordedModuleRegistration.flatMap(_.latestState),
        agreed = isAgreedSITS,
        history = recordedModuleRegistration.map(_.marks).getOrElse(Seq.empty),
        moduleRegistration = moduleRegistration,
        requiresResit = requiresResit
      )
    }
  }

  def studentModuleMarkRecords(
    sitsModuleCode: String,
    academicYear: AcademicYear,
    occurrence: String,
    moduleRegistrations: Seq[ModuleRegistration],
    moduleRegistrationMarksService: ModuleRegistrationMarksService,
    assessmentMembershipService: AssessmentMembershipService
  ): Seq[StudentModuleMarkRecord] = {
    val recordedModuleRegistrations = moduleRegistrationMarksService.getAllRecordedModuleRegistrations(sitsModuleCode, academicYear, occurrence)

    lazy val gradeBoundaries: Seq[GradeBoundary] = moduleRegistrations.map(_.marksCode).distinct.flatMap(assessmentMembershipService.markScheme)

    moduleRegistrations.sortBy(_.sprCode).map { moduleRegistration =>
      val recordedModuleRegistration = recordedModuleRegistrations.find(_.sprCode == moduleRegistration.sprCode)
      val process = if (moduleRegistration.currentResitAttempt.nonEmpty) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
      val grade = recordedModuleRegistration.flatMap(_.latestGrade)
      val gradeBoundary = grade.flatMap(g => gradeBoundaries.find(gb => gb.grade == g && gb.process == process))
      StudentModuleMarkRecord(moduleRegistration, recordedModuleRegistration, gradeBoundary.exists(_.generatesResit))
    }
  }

  type Result = Seq[ModuleOccurrence]
  type Command = Appliable[Result]

  def apply(department: Department, academicYear: AcademicYear, currentUser: CurrentUser): Command =
    new MarksDepartmentHomeCommandInternal(department, academicYear, currentUser)
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringMarksWorkflowProgressServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringResitServiceComponent
      with ComposableCommand[Result]
      with ListAssessmentComponentsModulesWithPermission
      with ListAssessmentComponentsPermissions
      with Unaudited with ReadOnly
}

abstract class MarksDepartmentHomeCommandInternal(val department: Department, val academicYear: AcademicYear, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with ListAssessmentComponentsState
    with ListAssessmentComponentsForModulesWithPermission
    with TaskBenchmarking {
  self: AssessmentComponentMarksServiceComponent
    with AssessmentMembershipServiceComponent
    with ResitServiceComponent
    with MarksWorkflowProgressServiceComponent
    with ListAssessmentComponentsModulesWithPermission
    with ModuleRegistrationServiceComponent
    with ModuleRegistrationMarksServiceComponent =>

  override def applyInternal(): Result = {
    val groupedInfo = benchmarkTask("Fetch and group assessment component info") {
      assessmentComponentInfos
        .groupBy { info => (info.assessmentComponent.moduleCode, info.upstreamAssessmentGroup.occurrence) }
    }

    val allModuleRegistrations = benchmarkTask("Get module registrations for department grouped by SITS module code, academicYear and occurrence") {
      moduleRegistrationService.getByDepartmentAndYear(department, academicYear)
        .groupBy(mr => (mr.sitsModuleCode, mr.academicYear, mr.occurrence))
    }

    benchmarkTask("Calculate progress for module") {
      groupedInfo.map { case ((moduleCode, occurrence), infos) => benchmarkTask(s"Process $moduleCode $occurrence") {
        val module = infos.head.assessmentComponent.module

        val moduleRegistrations = benchmarkTask("Get module registrations") { allModuleRegistrations.getOrElse((moduleCode, academicYear, occurrence), Seq.empty).sortBy(_.sprCode) }
        val students = benchmarkTask("Get student module mark records") {
          studentModuleMarkRecords(moduleCode, academicYear, occurrence, moduleRegistrations, moduleRegistrationMarksService, assessmentMembershipService)
        }

        val progress = benchmarkTask("Progress") { workflowProgressService.moduleOccurrenceProgress(students, infos) }

        ModuleOccurrence(
          moduleCode = moduleCode,
          module = module,
          occurrence = occurrence,

          progress = MarksWorkflowProgress(progress.percentage, progress.cssClass, progress.messageCode),
          nextStage = progress.nextStage,
          stages = progress.stages,

          assessmentComponents =
            infos.groupBy(_.assessmentComponent.assessmentGroup)
              .toSeq
              .sortBy { case (assessmentGroup, _) => assessmentGroup },
        )
      }}
      .toSeq.sortBy { mo => (mo.moduleCode, mo.occurrence) }
    }
  }
}
