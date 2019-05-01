package uk.ac.warwick.tabula.commands.mitcircs

import org.hibernate.criterion.Order
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.{AcademicYear, WorkflowStage, WorkflowStages}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.{MitigatingCircumstancesSubmissionFilter, ScalaRestriction}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesSubmission, MitigatingCircumstancesSubmissionState}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, AutowiringMitCircsWorkflowProgressServiceComponent, MitCircsSubmissionServiceComponent, MitCircsWorkflowProgressServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

case class MitigatingCircumstancesWorkflowProgress(percentage: Int, t: String, messageCode: String)

case class MitigatingCircumstancesSubmissionInfo(
  submission: MitigatingCircumstancesSubmission,
  progress: MitigatingCircumstancesWorkflowProgress,
  nextStage: Option[WorkflowStage],
  stages: ListMap[String, WorkflowStages.StageProgress],
)

case class AdminHomeInformation(
  submissions: Seq[MitigatingCircumstancesSubmissionInfo],
)

object AdminHomeCommand {
  type Command = Appliable[AdminHomeInformation] with AdminHomeCommandRequest
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Manage

  def apply(department: Department, year: AcademicYear): Command =
    new AdminHomeCommandInternal(department, year)
      with ComposableCommand[AdminHomeInformation]
      with AdminHomeCommandRequest
      with AutowiringMitCircsSubmissionServiceComponent
      with AutowiringMitCircsWorkflowProgressServiceComponent
      with AutowiringProfileServiceComponent
      with AdminHomePermissions
      with ReadOnly with Unaudited
}

abstract class AdminHomeCommandInternal(val department: Department, val year: AcademicYear) extends CommandInternal[AdminHomeInformation] with AdminHomeCommandState {
  self: AdminHomeCommandRequest
    with MitCircsSubmissionServiceComponent
    with MitCircsWorkflowProgressServiceComponent =>

  override def applyInternal(): AdminHomeInformation =
    AdminHomeInformation(
      submissions = mitCircsSubmissionService.submissionsForDepartment(
        department,
        buildRestrictions(year),
        MitigatingCircumstancesSubmissionFilter(
          affectedAssessmentModules = affectedAssessmentModules.asScala.toSet,
          includesStartDate = Option(includesStartDate),
          includesEndDate = Option(includesEndDate),
          approvedStartDate = Option(approvedStartDate),
          approvedEndDate = Option(approvedEndDate),
          state = state.asScala.toSet,
        )
      ).map { submission =>
        val progress = workflowProgressService.progress(department)(submission)

        MitigatingCircumstancesSubmissionInfo(
          submission = submission,
          progress = MitigatingCircumstancesWorkflowProgress(progress.percentage, progress.cssClass, progress.messageCode),
          nextStage = progress.nextStage,
          stages = progress.stages,
        )
      },
    )
}

trait AdminHomePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AdminHomeCommandState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(AdminHomeCommand.RequiredPermission, mandatory(department))
  }
}

trait AdminHomeCommandState {
  // This is used to filter the department the MCS is made to
  def department: Department

  // This is used as part of filtering the students whose MCS should display
  def year: AcademicYear
}

trait AdminHomeCommandRequest extends FiltersStudents with AdminHomeCommandState {
  // This is for filtering the student who has made the submission
  var courseTypes: JList[CourseType] = JArrayList()
  var routes: JList[Route] = JArrayList()
  var courses: JList[Course] = JArrayList()
  var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
  var yearsOfStudy: JList[JInteger] = JArrayList()
  var levelCodes: JList[String] = JArrayList()
  var sprStatuses: JList[SitsStatus] = JArrayList()

  // For filtering the submission itself
  var affectedAssessmentModules: JList[Module] = JArrayList()
  var includesStartDate: LocalDate = _
  var includesEndDate: LocalDate = _
  var approvedStartDate: LocalDate = _
  var approvedEndDate: LocalDate = _
  var state: JList[MitigatingCircumstancesSubmissionState] = JArrayList(MitigatingCircumstancesSubmissionState.Submitted)

  override val defaultOrder: Seq[Order] = Seq(Order.desc("lastModified"))
  override val sortOrder: JList[Order] = JArrayList() // Not used
  override val modules: JList[Module] = JArrayList() // Not used
  override val hallsOfResidence: JList[String] = JArrayList() // Not used

  override protected def buildRestrictions(year: AcademicYear): Seq[ScalaRestriction] =
    ScalaRestriction.is(
      "studentCourseYearDetails.academicYear", year,
      FiltersStudents.AliasPaths("studentCourseYearDetails"): _*
    ).toSeq ++ super.buildRestrictions(year)
}
