package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringScheme, AttendanceMonitoringPoint}

class DeleteSchemeCommandTest extends TestBase with Mockito {

	trait Fixture {

		val thisScheme = new AttendanceMonitoringScheme
		val point = new AttendanceMonitoringPoint
		thisScheme.points.add(point)

		val thisAttendanceMonitoringService = smartMock[AttendanceMonitoringService]

		val validator = new DeleteSchemeValidation with AttendanceMonitoringServiceComponent with DeleteSchemeCommandState {
			val scheme = thisScheme
			val user = currentUser
			val attendanceMonitoringService = thisAttendanceMonitoringService
		}
		var errors = new BindException(validator, "command")
	}

	@Test
	def validateNoCheckpoints() { new Fixture {
		validator.attendanceMonitoringService.countCheckpointsForPoint(point) returns (0)
		validator.validate(errors)
		errors.getAllErrors.size should be (0)
	}}

	@Test
	def validateHasCheckpoints() { new Fixture {
		validator.attendanceMonitoringService.countCheckpointsForPoint(point) returns (1)
		validator.validate(errors)
		errors.getAllErrors.size should be (1)
	}}

}
