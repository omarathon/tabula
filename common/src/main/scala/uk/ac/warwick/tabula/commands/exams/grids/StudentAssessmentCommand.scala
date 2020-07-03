package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.StudentAssessmentCommand.Command
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, AutowiringUpstreamRouteRuleServiceComponent, NormalCATSLoadServiceComponent, UpstreamRouteRuleServiceComponent}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent, AutowiringResitServiceComponent, ModuleRegistrationMarksServiceComponent, ResitServiceComponent}
import uk.ac.warwick.tabula.services.mitcircs.AutowiringMitCircsSubmissionServiceComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

object StudentAssessmentCommand {
  type Command = Appliable[StudentMarksBreakdown] with StudentAssessmentCommandState with PermissionsChecking

  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear): Command =
    new StudentAssessmentCommandInternal(studentCourseDetails, academicYear)
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringNormalCATSLoadServiceComponent
      with AutowiringUpstreamRouteRuleServiceComponent
      with AutowiringMitCircsSubmissionServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringResitServiceComponent
      with ComposableCommand[StudentMarksBreakdown]
      with StudentAssessmentPermissions
      with StudentAssessmentCommandState
      with StudentModuleRegistrationAndComponents
      with ReadOnly with Unaudited {
      override val includeActualMarks: Boolean = true

      override def mitCircsSubmissions: Option[Seq[MitigatingCircumstancesSubmission]] =
        Some(mitCircsSubmissionService.submissionsWithOutcomes(studentCourseDetails.student))
    }
}

object StudentAssessmentProfileCommand {
  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear): Command =
    new StudentAssessmentCommandInternal(studentCourseDetails, academicYear)
      with ComposableCommand[StudentMarksBreakdown]
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringNormalCATSLoadServiceComponent
      with AutowiringUpstreamRouteRuleServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringResitServiceComponent
      with StudentAssessmentProfilePermissions
      with StudentAssessmentCommandState
      with StudentModuleRegistrationAndComponents
      with ReadOnly with Unaudited {
      override val includeActualMarks: Boolean = false
    }
}

case class StudentMarksBreakdown(
  yearMark: Option[BigDecimal],
  weightedMeanYearMark: Option[BigDecimal],
  yearWeighting: Option[CourseYearWeighting],
  modules: Seq[ModuleRegistrationAndComponents],
  mitigatingCircumstances: Option[Seq[MitigatingCircumstancesSubmission]],
  progressionDecisions: Seq[ProgressionDecision],
)

case class ModuleRegistrationAndComponents(
  moduleRegistration: ModuleRegistration,
  markState: Option[MarkState],
  markRecord: StudentModuleMarkRecord,
  components: Seq[Component],
)

case class Component(
  upstreamGroup: UpstreamGroup,
  member: UpstreamAssessmentGroupMember,
  weighting: Option[BigDecimal],
  markState: Option[MarkState],
  markRecord: StudentMarkRecord,
)

class StudentAssessmentCommandInternal(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
  extends CommandInternal[StudentMarksBreakdown] with TaskBenchmarking {
  self: StudentAssessmentCommandState
    with StudentModuleRegistrationAndComponents
    with ModuleRegistrationServiceComponent
    with CourseAndRouteServiceComponent
    with NormalCATSLoadServiceComponent
    with UpstreamRouteRuleServiceComponent =>

  def mitCircsSubmissions: Option[Seq[MitigatingCircumstancesSubmission]] = None

  override def applyInternal(): StudentMarksBreakdown = {
    val modules = generateModuleRegistrationAndComponents(Seq(studentCourseYearDetails))

    val weightedMeanYearMark: Option[BigDecimal] =
      moduleRegistrationService.agreedWeightedMeanYearMark(studentCourseYearDetails.moduleRegistrations, Map(), allowEmpty = false).toOption

    val yearMark: Option[BigDecimal] = Option(studentCourseYearDetails.agreedMark).map(BigDecimal.apply).orElse {

      // overcatted marks are returned even if no agreed marks exist so map on weightedMeanYearMark to ensure that we are only showing "agreed" overcatt marks
      weightedMeanYearMark.flatMap(meanMark => {
        val normalLoad: BigDecimal =
          normalCATSLoadService.find(studentCourseYearDetails.route, academicYear, studentCourseYearDetails.yearOfStudy).map(_.normalLoad)
            .orElse(Option(studentCourseYearDetails.route).flatMap { r => Option(r.degreeType) }.map(_.normalCATSLoad))
            .getOrElse(DegreeType.Undergraduate.normalCATSLoad)

        val routeRules: Seq[UpstreamRouteRule] =
          studentCourseYearDetails.level.map { l =>
            upstreamRouteRuleService.list(studentCourseYearDetails.route, academicYear, l)
          }.getOrElse(Nil)

        val overcatSubsets: Seq[(BigDecimal, Seq[ModuleRegistration])] =
          moduleRegistrationService.overcattedModuleSubsets(studentCourseYearDetails.moduleRegistrations, Map(), normalLoad, routeRules)

        if (overcatSubsets.size > 1) {
          // If the student has overcatted and a subset of modules has been chosen for the overcatted mark,
          // find the subset that matches those modules, and show that mark if found
          studentCourseYearDetails.overcattingModules.flatMap(overcattingModules => {
            overcatSubsets
              .find { case (_, subset) => subset.size == overcattingModules.size && subset.map(_.module).forall(overcattingModules.contains) }
              .map { case (overcatMark, _) => Seq(meanMark, overcatMark).max }
          }).orElse(overcatSubsets.headOption.map(_._1).filter(_ > meanMark)) // if no subset has been chosen show the one with the highest mark
        } else {
          Option(meanMark)
        }
      })
    }

    val yearWeighting = courseAndRouteService.getCourseYearWeighting(
      studentCourseYearDetails.studentCourseDetails.course.code,
      studentCourseYearDetails.studentCourseDetails.sprStartAcademicYear,
      studentCourseYearDetails.yearOfStudy
    )

    // We only show progression decisions that are visible to the student, to prevent tutors accidentally leaking this
    val progressionDecisions =
      studentCourseDetails.progressionDecisionsByYear(academicYear)
        .filter(_.isVisibleToStudent)

    StudentMarksBreakdown(yearMark, weightedMeanYearMark, yearWeighting, modules, mitCircsSubmissions, progressionDecisions)
  }
}

trait StudentModuleRegistrationAndComponents {
  self: AssessmentMembershipServiceComponent with ModuleRegistrationMarksServiceComponent with AssessmentComponentMarksServiceComponent with ResitServiceComponent =>

  /**
   * Whether to consider actual marks, e.g. for calculating VAW weightings. This should be false when it will be displayed to students,
   * as they will be able to infer component marks before they are agreed from the variable weightings.
   */
  def includeActualMarks: Boolean

  def generateModuleRegistrationAndComponents(scyds: Seq[StudentCourseYearDetails]): Seq[ModuleRegistrationAndComponents] = {

    val studentsResits: Seq[RecordedResit] = resitService.findResits(scyds.map(_.studentCourseDetails.sprCode))

    lazy val gradeBoundaries: Seq[GradeBoundary] = {
      val mr = scyds.flatMap(_.moduleRegistrations)
      val ac = mr.flatMap(_.upstreamAssessmentGroups).flatMap(_.assessmentComponent)
      val marksCodes = (mr.map(_.marksCode) ++ ac.map(_.marksCode)).distinct
      marksCodes.flatMap(assessmentMembershipService.markScheme)
    }

    scyds.flatMap { scyd =>
      scyd.moduleRegistrations.map { mr =>
        val components: Seq[(UpstreamGroup, UpstreamAssessmentGroupMember)] =
          for {
            uagm <- mr.upstreamAssessmentGroupMembers
            aComponent <- uagm.upstreamAssessmentGroup.assessmentComponent
          } yield (new UpstreamGroup(aComponent, uagm.upstreamAssessmentGroup, mr.currentUpstreamAssessmentGroupMembers), uagm)

        // For VAW
        val marks: Seq[(AssessmentType, String, Option[Int])] = mr.componentMarks(includeActualMarks = includeActualMarks)
        val hasAnyMarks = marks.exists { case (_, _, mark) => mark.nonEmpty }

        val recordedModuleRegistration: Option[RecordedModuleRegistration] = moduleRegistrationMarksService.getRecordedModuleRegistration(mr)
        val process = if (mr.currentResitAttempt.nonEmpty) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
        val grade = recordedModuleRegistration.flatMap(_.latestGrade)
        val gradeBoundary = grade.flatMap(g => gradeBoundaries.find(gb => gb.grade == g && gb.process == process && mr.marksCode == gb.marksCode))

        ModuleRegistrationAndComponents(
          moduleRegistration = mr,
          markState = recordedModuleRegistration.flatMap(_.latestState),
          markRecord = StudentModuleMarkRecord(mr, recordedModuleRegistration, gradeBoundary.exists(_.generatesResit)),
          components = components.map { case (ug, uagm) =>
            val recordedAssessmentComponentStudent: Option[RecordedAssessmentComponentStudent] = assessmentComponentMarksService.getRecordedStudent(uagm)
            val resit: Option[RecordedResit] = studentsResits.filter(r => r.sprCode == mr.sprCode && r.sequence == ug.sequence)
              .sortBy(_.resitSequence)
              .headOption
            val gradeBoundary = {
              val process = if (uagm.isReassessment) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
              val grade = recordedAssessmentComponentStudent.flatMap(_.latestGrade)
              grade.flatMap(g => gradeBoundaries.find(gb => gb.grade == g && gb.process == process))
            }

            Component(
              upstreamGroup = ug,
              member = uagm,
              weighting = if (hasAnyMarks) ug.assessmentComponent.weightingFor(marks) else ug.assessmentComponent.scaledWeighting,
              markState = recordedAssessmentComponentStudent.flatMap(_.latestState),
              markRecord = StudentMarkRecord(
                UpstreamAssessmentGroupInfo(
                  uagm.upstreamAssessmentGroup,
                  Seq(uagm).filterNot(_ => scyd.studentCourseDetails.permanentlyWithdrawn)
                ),
                uagm,
                recordedAssessmentComponentStudent,
                resit,
                gradeBoundary.exists(_.generatesResit)
              ),
            )
          }
        )
      }
    }
  }
}

trait StudentAssessmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: StudentAssessmentCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheckAny(
      Seq(CheckablePermission(Permissions.Department.ExamGrids, studentCourseYearDetails.enrolmentDepartment),
        CheckablePermission(Permissions.Department.ExamGrids, studentCourseYearDetails.studentCourseDetails.currentRoute))
    )
  }

}

trait StudentAssessmentProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentAssessmentCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Profiles.Read.ModuleRegistration.Core, studentCourseDetails)
  }
}


trait StudentAssessmentCommandState {
  def academicYear: AcademicYear

  def studentCourseDetails: StudentCourseDetails

  lazy val studentCourseYearDetails: StudentCourseYearDetails = studentCourseDetails.freshStudentCourseYearDetailsForYear(academicYear) // fresh scyd for this year
    .orElse(studentCourseDetails.freshOrStaleStudentCourseYearDetailsForYear(academicYear)) // or stale scyd for this year
    .getOrElse(throw new ItemNotFoundException())
}
