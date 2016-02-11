package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AttendanceMonitoringService}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.BindException

class AddStudentsToSchemeCommandTest extends TestBase with Mockito {

	trait Fixture {
		val thisScheme = new AttendanceMonitoringScheme
		val staticStudent = Fixtures.student("1234")
		val includeStudent = Fixtures.student("2345")
		val excludeStudent = Fixtures.student("3456")
		val updatedStaticStudent = Fixtures.student("1234")
		val updatedIncludeStudent = Fixtures.student("9999")
		val updatedExcludeStudent = Fixtures.student("8888")
		val thisProfileService = smartMock[ProfileService]
		val thisSecurityService = smartMock[SecurityService]
		val thisAttendanceMonitoringService = smartMock[AttendanceMonitoringService]

	}

	@Test
	def validateOk() = withUser("cusfal") { new Fixture {
		val validator = new AddStudentsToSchemeValidation with AddStudentsToSchemeCommandState
			with AttendanceMonitoringServiceComponent with ProfileServiceComponent with SecurityServiceComponent {

			val scheme = thisScheme
			val user = currentUser
			val profileService = thisProfileService
			val securityService = thisSecurityService
			val attendanceMonitoringService = thisAttendanceMonitoringService
		}

		validator.staticStudentIds.add(staticStudent.universityId)
		validator.includedStudentIds.add(includeStudent.universityId)
		validator.excludedStudentIds.add(excludeStudent.universityId)

		thisProfileService.getAllMembersWithUniversityIds(
			Seq(staticStudent.universityId, includeStudent.universityId)
		) returns Seq(staticStudent, includeStudent)

		thisSecurityService.can(currentUser, Permissions.MonitoringPoints.Manage, staticStudent) returns true
		thisSecurityService.can(currentUser, Permissions.MonitoringPoints.Manage, includeStudent) returns true

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.getAllErrors.size should be (0)

	}}

	@Test
	def validateNotOk() { withUser("cusfal") { new Fixture {
		val validator = new AddStudentsToSchemeValidation with AddStudentsToSchemeCommandState
			with AttendanceMonitoringServiceComponent with ProfileServiceComponent with SecurityServiceComponent {

			val scheme = thisScheme
			val user = currentUser
			val profileService = thisProfileService
			val securityService = thisSecurityService
			val attendanceMonitoringService = thisAttendanceMonitoringService
		}

		validator.staticStudentIds.add(staticStudent.universityId)
		validator.includedStudentIds.add(includeStudent.universityId)
		validator.excludedStudentIds.add(excludeStudent.universityId)

		thisProfileService.getAllMembersWithUniversityIds(
			Seq(staticStudent.universityId, includeStudent.universityId)
		) returns Seq(staticStudent, includeStudent)

		thisSecurityService.can(currentUser, Permissions.MonitoringPoints.Manage, staticStudent) returns true
		thisSecurityService.can(currentUser, Permissions.MonitoringPoints.Manage, includeStudent) returns false

		var errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.getAllErrors.size should be (1)

	}}}

}
