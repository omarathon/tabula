package uk.ac.warwick.tabula.commands.exams.grids

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent, ModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object GenerateModuleExamGridCommand {
  def apply(department: Department, academicYear: AcademicYear) =
    new GenerateModuleExamGridCommandInternal(department, academicYear)
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringStudentCourseYearDetailsDaoComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with ComposableCommand[ModuleExamGridResult]
      with GenerateModuleExamGridValidation
      with GenerateModuleExamGridPermissions
      with GenerateModuleExamGridCommandState
      with GenerateModuleExamGridCommandRequest
      with ReadOnly with Unaudited
}

case class ModuleExamGridResult(
  upstreamAssessmentGroupAndSequenceAndOccurrencesWithComponentName: Seq[(String, String)],
  gridStudentDetailRecords: Seq[ModuleGridDetailRecord]
)

case class ModuleGridDetailRecord(
  moduleRegistration: ModuleRegistration,
  componentInfo: Map[String, AssessmentComponentInfo],
  name: String,
  universityId: String,
  lastImportDate: Option[DateTime]
)


case class AssessmentComponentInfo(mark: Option[Int], grade: Option[String], isActualMark: Boolean, isActualGrade: Boolean, markState: Option[MarkState], resitInfo: ResitComponentInfo)

case class ResitComponentInfo(resitMark: Option[Int], resitGrade: Option[String], isActualResitMark: Boolean, isActualResitGrade: Boolean)

case class AssessmentIdentity(code: String, name: String)

class GenerateModuleExamGridCommandInternal(val department: Department, val academicYear: AcademicYear)
  extends CommandInternal[ModuleExamGridResult] with TaskBenchmarking {

  self: StudentCourseYearDetailsDaoComponent with GenerateModuleExamGridCommandRequest with ModuleRegistrationServiceComponent
    with AssessmentMembershipServiceComponent with ModuleRegistrationMarksServiceComponent with AssessmentComponentMarksServiceComponent =>

  override def applyInternal(): ModuleExamGridResult = {
    val isHistoricalGrid = academicYear != AcademicYear.now()
    val result: Seq[(AssessmentIdentity, ModuleGridDetailRecord)] = benchmarkTask("GenerateMRComponents") {
      moduleRegistrationService.getByModuleAndYear(module, academicYear)
        .groupBy(_.studentCourseDetails.student)
        .view.mapValues(_.sortBy(_.sprSequence).reverse).toMap
        // multiple registrations here should only be the result of a course transfer - pick the highest spr
        .flatMap { case (_, registrations) => registrations.headOption }.toSeq
        .flatMap { mr =>
          val student: StudentMember = mr.studentCourseDetails.student
          val componentInfo: Seq[(AssessmentIdentity, AssessmentComponentInfo)] = mr.upstreamAssessmentGroupMembers.flatMap { uagm =>
            val code = s"${uagm.upstreamAssessmentGroup.assessmentGroup}-${uagm.upstreamAssessmentGroup.sequence}-${uagm.upstreamAssessmentGroup.occurrence}"
            assessmentMembershipService.getAssessmentComponent(uagm.upstreamAssessmentGroup)
              .filter(ac => isHistoricalGrid || ac.inUse) // TAB-8263 only filter 'not in use' components for this years grids
              .map { comp => AssessmentIdentity(code = code, name = comp.name) -> AssessmentComponentInfo(
                mark = uagm.firstOriginalMark,
                grade = uagm.firstOriginalGrade,
                isActualMark = uagm.agreedMark.isEmpty,
                isActualGrade = uagm.agreedGrade.isEmpty,
                markState = assessmentComponentMarksService.getRecordedStudent(uagm).flatMap(_.latestState),
                resitInfo = ResitComponentInfo(
                  resitMark = uagm.firstDefinedMark.filter(_ => uagm.isReassessment),
                  resitGrade = uagm.firstDefinedGrade.filter(_ => uagm.isReassessment),
                  isActualResitMark = uagm.isReassessment && uagm.agreedMark.isEmpty,
                  isActualResitGrade = uagm.isReassessment && uagm.agreedGrade.isEmpty
                )
              )}
          }.sortBy { case (assessmentIdentity, _) => assessmentIdentity.code }

          componentInfo.map { case (assessmentIdentity, _) => assessmentIdentity ->
            ModuleGridDetailRecord(
              moduleRegistration = mr,
              componentInfo = componentInfo.map { case (id, info) => (id.code, info) }.toMap,
              name = s"${student.firstName} ${student.lastName}",
              universityId = student.universityId,
              lastImportDate = Option(student.lastImportDate)
            )
          }
        }
    }

    ModuleExamGridResult(
      upstreamAssessmentGroupAndSequenceAndOccurrencesWithComponentName = result.toMap.keys.toSeq.map(assessmentIdentity =>
        (assessmentIdentity.code, assessmentIdentity.name)),
      gridStudentDetailRecords = result.map { case (_, records) => records }.distinct
    )
  }
}

trait GenerateModuleExamGridValidation extends SelfValidating {

  self: GenerateModuleExamGridCommandState with GenerateModuleExamGridCommandRequest =>
  override def validate(errors: Errors): Unit = {
    if (module == null) {
      errors.reject("examModuleGrid.module.empty")
    }
  }
}

trait GenerateModuleExamGridPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: GenerateModuleExamGridCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Department.ExamGrids, department)
  }

}

trait GenerateModuleExamGridCommandState {

  self: CourseAndRouteServiceComponent =>

  def department: Department

  def academicYear: AcademicYear

}

trait GenerateModuleExamGridCommandRequest {
  var module: Module = _
}
