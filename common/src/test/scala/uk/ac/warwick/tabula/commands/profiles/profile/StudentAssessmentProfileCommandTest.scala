package uk.ac.warwick.tabula.commands.profiles.profile

import uk.ac.warwick.tabula.commands.exams.grids.{StudentAssessmentCommandState, StudentAssessmentProfileCommand, StudentAssessmentProfilePermissions}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}

class StudentAssessmentProfileCommandTest extends TestBase with Mockito {

  val testStudent = new StudentMember
  val scd = new StudentCourseDetails(testStudent, "student")
  val year = AcademicYear(2014)

  @Test
  def requiresModuleRegistrationCoreReadPermissions(): Unit = {
    val perms: StudentAssessmentProfilePermissions with StudentAssessmentCommandState = new StudentAssessmentProfilePermissions with StudentAssessmentCommandState {
      val studentCourseDetails: StudentCourseDetails = scd
      val academicYear: AcademicYear = year
    }

    val checking = mock[PermissionsChecking]
    perms.permissionsCheck(checking)
    verify(checking, times(1)).PermissionCheck(Profiles.Read.ModuleRegistration.Core, scd)
  }

  @Test
  def mixesCorrectPermissionsIntoCommand(): Unit = {
    val composedCommand = StudentAssessmentProfileCommand(scd, year)
    composedCommand should be(anInstanceOf[StudentAssessmentProfilePermissions])
  }

}
