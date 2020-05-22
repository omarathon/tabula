package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksService, AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

object ListAssessmentComponentsCommand {
  case class StudentMarkRecord(
    universityId: String,
    position: Option[Int],
    currentMember: Boolean,
    mark: Option[Int],
    grade: Option[String],
    needsWritingToSits: Boolean,
    outOfSync: Boolean,
    agreed: Boolean,
    history: Seq[RecordedAssessmentComponentStudentMark] // Most recent first
  )
  object StudentMarkRecord {
    def apply(info: UpstreamAssessmentGroupInfo, member: UpstreamAssessmentGroupMember, recordedStudent: Option[RecordedAssessmentComponentStudent]): StudentMarkRecord =
      StudentMarkRecord(
        universityId = member.universityId,
        position = member.position,
        currentMember = info.currentMembers.contains(member),
        mark =
          recordedStudent.filter(_.needsWritingToSits).flatMap(_.latestMark)
            .orElse(member.firstAgreedMark)
            .orElse(recordedStudent.flatMap(_.latestMark))
            .orElse(member.firstDefinedMark),
        grade =
          recordedStudent.filter(_.needsWritingToSits).flatMap(_.latestGrade)
            .orElse(member.firstAgreedGrade)
            .orElse(recordedStudent.flatMap(_.latestGrade))
            .orElse(member.firstDefinedGrade),
        needsWritingToSits = recordedStudent.exists(_.needsWritingToSits),
        outOfSync =
          recordedStudent.exists(!_.needsWritingToSits) && (
            recordedStudent.flatMap(_.latestMark).exists(m => !member.firstDefinedMark.contains(m)) ||
            recordedStudent.flatMap(_.latestGrade).exists(g => !member.firstDefinedGrade.contains(g))
          ),
        agreed = recordedStudent.forall(!_.needsWritingToSits) && member.firstAgreedMark.nonEmpty,
        history = recordedStudent.map(_.marks).getOrElse(Seq.empty),
      )
  }

  def studentMarkRecords(info: UpstreamAssessmentGroupInfo, assessmentComponentMarksService: AssessmentComponentMarksService): Seq[StudentMarkRecord] = {
    val recordedStudents = assessmentComponentMarksService.getAllRecordedStudents(info.upstreamAssessmentGroup)

    info.allMembers.sortBy(_.universityId).map { member =>
      val recordedStudent = recordedStudents.find(_.universityId == member.universityId)

      StudentMarkRecord(info, member, recordedStudent)
    }
  }

  case class AssessmentComponentInfo(
    assessmentComponent: AssessmentComponent,
    upstreamAssessmentGroup: UpstreamAssessmentGroup,
    students: Seq[StudentMarkRecord]
  ) {
    val studentsWithMarks: Seq[StudentMarkRecord] = students.filter(s => s.mark.nonEmpty || s.grade.nonEmpty)

    val needsWritingToSits: Boolean = students.exists(_.needsWritingToSits)
    val outOfSync: Boolean = students.exists(_.outOfSync)
    val allAgreed: Boolean = students.nonEmpty && students.forall(_.agreed)
  }
  type Result = Seq[AssessmentComponentInfo]
  type Command = Appliable[Result]

  val AdminPermission: Permission = Permissions.Feedback.Manage

  def apply(department: Department, academicYear: AcademicYear, currentUser: CurrentUser): Command =
    new ListAssessmentComponentsCommandInternal(department, academicYear, currentUser)
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with ComposableCommand[Result]
      with ListAssessmentComponentsModulesWithPermission
      with ListAssessmentComponentsPermissions
      with Unaudited with ReadOnly
}

abstract class ListAssessmentComponentsCommandInternal(val department: Department, val academicYear: AcademicYear, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with ListAssessmentComponentsState {
  self: AssessmentComponentMarksServiceComponent
    with AssessmentMembershipServiceComponent
    with ListAssessmentComponentsModulesWithPermission
    with SecurityServiceComponent
    with ModuleAndDepartmentServiceComponent =>

  override def applyInternal(): Result = {
    val assessmentComponents: Seq[AssessmentComponent] =
      assessmentMembershipService.getAssessmentComponents(department, includeSubDepartments = false)
        .filter { ac =>
          ac.assessmentGroup != "AO" &&
          ac.sequence != AssessmentComponent.NoneAssessmentGroup &&
          (canAdminDepartment || modulesWithPermission.contains(ac.module))
        }

    val assessmentComponentsByKey: Map[AssessmentComponentKey, AssessmentComponent] =
      assessmentComponents.map { ac =>
        AssessmentComponentKey(ac) -> ac
      }.toMap

    assessmentMembershipService.getUpstreamAssessmentGroupInfoForComponents(assessmentComponents, academicYear)
      .filter(_.allMembers.nonEmpty)
      .map { upstreamAssessmentGroupInfo =>
        AssessmentComponentInfo(
          assessmentComponentsByKey(AssessmentComponentKey(upstreamAssessmentGroupInfo.upstreamAssessmentGroup)),
          upstreamAssessmentGroupInfo.upstreamAssessmentGroup,
          studentMarkRecords(upstreamAssessmentGroupInfo, assessmentComponentMarksService)
        )
      }
      .sortBy { info =>
        // module_code, assessment_group, sequence, mav_occurrence
        (info.assessmentComponent.moduleCode, info.assessmentComponent.assessmentGroup, info.assessmentComponent.sequence, info.upstreamAssessmentGroup.occurrence)
      }
  }

}

trait ListAssessmentComponentsState {
  def department: Department
  def academicYear: AcademicYear
  def currentUser: CurrentUser
}

trait ListAssessmentComponentsModulesWithPermission {
  self: ListAssessmentComponentsState
    with SecurityServiceComponent
    with ModuleAndDepartmentServiceComponent =>

  lazy val canAdminDepartment: Boolean = securityService.can(currentUser, AdminPermission, department)
  lazy val modulesWithPermission: Set[Module] = moduleAndDepartmentService.modulesWithPermission(currentUser, AdminPermission, department)
}

trait ListAssessmentComponentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ListAssessmentComponentsModulesWithPermission
    with ListAssessmentComponentsState
    with SecurityServiceComponent
    with ModuleAndDepartmentServiceComponent =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    if (canAdminDepartment || modulesWithPermission.isEmpty) p.PermissionCheck(AdminPermission, mandatory(department))
    else p.PermissionCheckAll(AdminPermission, modulesWithPermission)
}
