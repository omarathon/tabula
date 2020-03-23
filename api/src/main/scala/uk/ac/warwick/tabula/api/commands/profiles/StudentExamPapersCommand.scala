package uk.ac.warwick.tabula.api.commands.profiles

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.Component
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object StudentExamPapersCommand {

  def apply(studentMember: StudentMember, academicYear: AcademicYear) =
    new StudentExamPapersCommandInternal(studentMember, academicYear)
      with ComposableCommand[Seq[ExamModuleRegistrationAndComponents]]
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with StudentExamPapersPermissions
      with StudentExamPapersCommandState
      with ReadOnly with Unaudited
}

case class ExamModuleRegistrationAndComponents(
  moduleRegistration: ModuleRegistration,
  components: Seq[Component]
)


class StudentExamPapersCommandInternal(val studentMember: StudentMember, val academicYear: AcademicYear)
  extends CommandInternal[Seq[ExamModuleRegistrationAndComponents]] with TaskBenchmarking {
  self: StudentExamPapersCommandState
    with AssessmentMembershipServiceComponent
    with ModuleRegistrationServiceComponent =>

  override def applyInternal(): Seq[ExamModuleRegistrationAndComponents] = {
    studentCourseYearDetails.flatMap { scyd =>
      scyd.moduleRegistrations.map { mr =>
        val components = for {
          uagm <- mr.upstreamAssessmentGroupMembers
          aComponent <- assessmentMembershipService.getAssessmentComponent(uagm.upstreamAssessmentGroup)
        } yield Component(new UpstreamGroup(aComponent, uagm.upstreamAssessmentGroup, mr.currentUpstreamAssessmentGroupMembers), uagm)

        val examComponents = components.filter(c => c.upstreamGroup.assessmentComponent.examPaperCode.isDefined)
        ExamModuleRegistrationAndComponents(mr, examComponents)
      }.filter(_.components.nonEmpty)
    }
  }
}


trait StudentExamPapersPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentExamPapersCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Profiles.Read.ModuleRegistration.Core, studentMember)
  }
}


trait StudentExamPapersCommandState {
  def academicYear: AcademicYear

  def studentMember: StudentMember

  lazy val studentCourseYearDetails: Seq[StudentCourseYearDetails] = studentMember.freshStudentCourseDetails.flatMap { scd => scd.freshStudentCourseYearDetailsForYear(academicYear) } // fresh scyd for this year

}
