package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.{AssessmentComponentInfo, studentMarkRecords}
import uk.ac.warwick.tabula.commands.sysadmin.ComponentMarkUploadProgressCommand.{ComponentMarkUploadProgress, _}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, AssessmentComponentKey, Department}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringAssessmentMembershipServiceComponent, AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ComponentMarkUploadProgressCommand {

  type Result = Seq[ComponentMarkUploadProgressResult]
  type Command = Appliable[Result]
  val RequiredPermission: Permission = Permissions.Marks.ReviewComponentMarksUpload

  case class ComponentMarkUploadProgress(
    assessmentComponentCount: Int,
    totalStudentsCount: Int,
    studentsWithMarksCount: Int,
    percentageComplete: String
  )

  case class ComponentMarkUploadProgressResult(
    department: Department,
    componentMarkUploadProgress: ComponentMarkUploadProgress
  )

  def apply() = new ComponentMarkUploadProgressCommandInternal()
    with ComposableCommand[Result]
    with ComponentMarkUploadProgressPermissions
    with AutowiringAssessmentMembershipServiceComponent
    with AutowiringAssessmentComponentMarksServiceComponent
    with AutowiringModuleAndDepartmentServiceComponent
    with Unaudited with ReadOnly
}

class ComponentMarkUploadProgressCommandInternal() extends CommandInternal[Result] {

  self: AutowiringAssessmentMembershipServiceComponent with AssessmentComponentMarksServiceComponent with ModuleAndDepartmentServiceComponent =>

  def applyInternal(): Result = transactional() {

    moduleAndDepartmentService.allDepartments.toSet.map { department:Department =>
      val assessmentComponents = assessmentMembershipService.getAssessmentComponents(moduleAndDepartmentService.getDepartmentByCode(department.code).head, includeSubDepartments = false)
        .filter { ac =>
          ac.assessmentGroup != "AO" &&
            ac.sequence != AssessmentComponent.NoneAssessmentGroup
        }
      val assessmentComponentsByKey: Map[AssessmentComponentKey, AssessmentComponent] =
        assessmentComponents.map { ac =>
          AssessmentComponentKey(ac) -> ac
        }.toMap

      val assessmentComponentInfo: Seq[AssessmentComponentInfo] = {
        assessmentMembershipService.getUpstreamAssessmentGroupInfoForComponents(assessmentComponents, AcademicYear.now())
          .filter(_.allMembers.nonEmpty)
          .map { upstreamAssessmentGroupInfo =>
            AssessmentComponentInfo(
              assessmentComponentsByKey(AssessmentComponentKey(upstreamAssessmentGroupInfo.upstreamAssessmentGroup)),
              upstreamAssessmentGroupInfo.upstreamAssessmentGroup,
              studentMarkRecords(upstreamAssessmentGroupInfo, assessmentComponentMarksService)
            )
          }
      }

      val totalStudents = assessmentComponentInfo.map(_.students.size).sum
      val studentsWithMarks = assessmentComponentInfo.map(_.studentsWithMarks.size).sum
      val percentageComplete = Math.ceil((studentsWithMarks.toFloat / totalStudents.toFloat) * 100).toInt
      ComponentMarkUploadProgressResult(department,
        ComponentMarkUploadProgress(
          assessmentComponentInfo.size,
          totalStudents,
          studentsWithMarks,
          percentageComplete =  if (totalStudents > 0) s"$percentageComplete%" else ""
        )
      )
    }.toSeq.filter(_.componentMarkUploadProgress.assessmentComponentCount > 0).sortBy(_.department.name)
  }
}

  trait ComponentMarkUploadProgressPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

    def permissionsCheck(p: PermissionsChecking): Unit = {
      p.PermissionCheck(RequiredPermission, PermissionsTarget.Global)
    }
  }

