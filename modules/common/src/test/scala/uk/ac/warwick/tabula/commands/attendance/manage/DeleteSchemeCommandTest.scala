package uk.ac.warwick.tabula.commands.attendance.manage

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.services.{ProfileService, ProfileServiceComponent, ScheduledNotificationService}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class DeleteSchemeCommandTest extends TestBase with Mockito {

	trait Fixture {
		val thisDepartment = Fixtures.department("its")
		val thisAcademicYear = AcademicYear(2014)
		val student = Fixtures.student("1234")
		val thisScheme = new AttendanceMonitoringScheme
		thisScheme.department = thisDepartment
		thisScheme.academicYear = thisAcademicYear
		thisScheme.members.addUserId(student.universityId)
		val point = new AttendanceMonitoringPoint
		thisScheme.points.add(point)

		val thisAttendanceMonitoringService = smartMock[AttendanceMonitoringService]

		val validator = new DeleteSchemeValidation with AttendanceMonitoringServiceComponent with DeleteSchemeCommandState {
			val scheme = thisScheme
			val user = currentUser
			val attendanceMonitoringService = thisAttendanceMonitoringService
		}
		var errors = new BindException(validator, "command")

		val cmd = new DeleteSchemeCommandInternal(thisScheme) with DeleteSchemeCommandState
			with AttendanceMonitoringServiceComponent with ProfileServiceComponent {
			override val attendanceMonitoringService = smartMock[AttendanceMonitoringService]
			override val profileService = smartMock[ProfileService]
			thisScheduledNotificationService = smartMock[ScheduledNotificationService]
		}
	}

	@Test
	def validateNoCheckpoints() { new Fixture {
		validator.attendanceMonitoringService.countCheckpointsForPoint(point) returns 0
		validator.validate(errors)
		errors.getAllErrors.size should be (0)
	}}

	@Test
	def validateHasCheckpoints() { new Fixture {
		validator.attendanceMonitoringService.countCheckpointsForPoint(point) returns 1
		validator.validate(errors)
		errors.getAllErrors.size should be (1)
	}}

	@Test
	def applyInternal(): Unit = new Fixture {
		cmd.attendanceMonitoringService.listAllSchemes(thisDepartment) returns Seq()
		cmd.profileService.getAllMembersWithUniversityIds(Seq(student.universityId)) returns Seq(student)
		cmd.applyInternal()
		verify(cmd.attendanceMonitoringService, times(1)).setCheckpointTotalsForUpdate(Seq(student), thisDepartment, thisAcademicYear)
	}

}
