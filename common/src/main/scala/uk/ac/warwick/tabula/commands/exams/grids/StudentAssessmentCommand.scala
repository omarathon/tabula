package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, AutowiringUpstreamRouteRuleServiceComponent, NormalCATSLoadServiceComponent, UpstreamRouteRuleServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object StudentAssessmentCommand {
  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
    new StudentAssessmentCommandInternal(studentCourseDetails, academicYear)
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringNormalCATSLoadServiceComponent
      with AutowiringUpstreamRouteRuleServiceComponent
      with ComposableCommand[StudentMarksBreakdown]
      with StudentAssessmentPermissions
      with StudentAssessmentCommandState
      with ReadOnly with Unaudited
}

object StudentAssessmentProfileCommand {

  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
    new StudentAssessmentCommandInternal(studentCourseDetails, academicYear)
      with ComposableCommand[StudentMarksBreakdown]
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringNormalCATSLoadServiceComponent
      with AutowiringUpstreamRouteRuleServiceComponent
      with StudentAssessmentProfilePermissions
      with StudentAssessmentCommandState
      with ReadOnly with Unaudited
}

case class StudentMarksBreakdown(
  yearMark: Option[BigDecimal],
  weightedMeanYearMark: Option[BigDecimal],
  yearWeighting: Option[CourseYearWeighting],
  modules: Seq[ModuleRegistrationAndComponents]
)

case class ModuleRegistrationAndComponents(
  moduleRegistration: ModuleRegistration,
  components: Seq[Component]
)

case class Component(upstreamGroup: UpstreamGroup, member: UpstreamAssessmentGroupMember)

class StudentAssessmentCommandInternal(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
  extends CommandInternal[StudentMarksBreakdown] with TaskBenchmarking {
  self: StudentAssessmentCommandState
    with AssessmentMembershipServiceComponent
    with ModuleRegistrationServiceComponent
    with CourseAndRouteServiceComponent
    with NormalCATSLoadServiceComponent
    with UpstreamRouteRuleServiceComponent =>

  override def applyInternal(): StudentMarksBreakdown = {
    val modules = studentCourseYearDetails.moduleRegistrations.map { mr =>
      val components = for {
        uagm <- mr.upstreamAssessmentGroupMembers
        aComponent <- assessmentMembershipService.getAssessmentComponent(uagm.upstreamAssessmentGroup)
      } yield Component(new UpstreamGroup(aComponent, uagm.upstreamAssessmentGroup, mr.currentUpstreamAssessmentGroupMembers), uagm)
      ModuleRegistrationAndComponents(mr, components)
    }

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

        if(overcatSubsets.size > 1) {
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

    StudentMarksBreakdown(yearMark, weightedMeanYearMark, yearWeighting, modules)
  }
}


trait StudentAssessmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: StudentAssessmentCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheckAny(
      Seq(CheckablePermission(Permissions.Department.ExamGrids, studentCourseYearDetails.enrolmentDepartment),
        CheckablePermission(Permissions.Department.ExamGrids, studentCourseYearDetails.studentCourseDetails.currentRoute))
    )
  }

}

trait StudentAssessmentProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentAssessmentCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
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
