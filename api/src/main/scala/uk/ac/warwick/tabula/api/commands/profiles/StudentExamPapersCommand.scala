package uk.ac.warwick.tabula.api.commands.profiles

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.{ModuleRegistrationAndComponents, StudentModuleRegistrationAndComponents}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.marks.{AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object StudentExamPapersCommand {

  def apply(studentMember: StudentMember, academicYear: AcademicYear) =
    new StudentExamPapersCommandInternal(studentMember, academicYear)
      with ComposableCommand[Seq[ModuleRegistrationAndComponents]]
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with StudentExamPapersPermissions
      with StudentExamPapersCommandState
      with StudentModuleRegistrationAndComponents
      with ReadOnly with Unaudited {
      override val includeActualMarks: Boolean = false
    }
}


class StudentExamPapersCommandInternal(val studentMember: StudentMember, val academicYear: AcademicYear)
  extends CommandInternal[Seq[ModuleRegistrationAndComponents]] with TaskBenchmarking {
  self: StudentExamPapersCommandState
    with StudentModuleRegistrationAndComponents
    with ModuleRegistrationServiceComponent =>


  override def applyInternal(): Seq[ModuleRegistrationAndComponents] = {
    generateModuleRegistrationAndComponents(studentCourseYearDetails).flatMap { moduleRegistrationAndComponents =>
      val examComponents = moduleRegistrationAndComponents.components.filter(c => c.upstreamGroup.assessmentComponent.examPaperCode.isDefined)
      if (examComponents.nonEmpty) {
        Some(moduleRegistrationAndComponents.copy(components = examComponents))
      } else {
        None
      }
    }
  }
}


trait StudentExamPapersPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentExamPapersCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Profiles.Read.ModuleRegistration.Core, studentMember)
    p.PermissionCheck(Profiles.Read.StudentCourseDetails.SpecialExamArrangements, studentMember)
  }
}


trait StudentExamPapersCommandState {
  def academicYear: AcademicYear

  def studentMember: StudentMember

  lazy val studentCourseYearDetails: Seq[StudentCourseYearDetails] = studentMember.freshStudentCourseDetails.flatMap { scd => scd.freshStudentCourseYearDetailsForYear(academicYear) } // fresh scyd for this year

}
