package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentCourseYearDetails, StudentMember}

import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions.Profiles

class ViewModuleRegistrationsCommandTest extends TestBase with Mockito {

	val testStudent = new StudentMember
	val scd = new StudentCourseDetails(testStudent, "student")
	val scyd = new StudentCourseYearDetails(scd, 1, AcademicYear(2014))

	@Test
	def requiresModuleRegistrationCoreReadPermissions() {
		val perms = new ViewModuleRegistrationsCommandPermissions with ViewModuleRegistrationsCommandState {
			val student = testStudent
			val studentCourseYearDetails = scyd
		}

		val checking = mock[PermissionsChecking]
		perms.permissionsCheck(checking)
		there was one(checking).PermissionCheck(Profiles.Read.ModuleRegistration.Core, testStudent)
	}

	@Test
	def mixesCorrectPermissionsIntoCommand() {
		val composedCommand = ViewModuleRegistrationsCommand(scyd)
		composedCommand should be(anInstanceOf[ViewModuleRegistrationsCommandPermissions])
	}

}
