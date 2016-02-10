package uk.ac.warwick.tabula.commands.attendance.manage

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.{Mockito, TestBase}

class DeleteAttendancePointCommandTest extends TestBase with Mockito {

	trait Fixture {

		val point = new AttendanceMonitoringPoint
		val validator = new DeleteAttendancePointValidation with AttendanceMonitoringServiceComponent with DeleteAttendancePointCommandState {
			val attendanceMonitoringService = smartMock[AttendanceMonitoringService]
			def department = null
			def templatePoint = null
			override def pointsToDelete = Seq(point)
		}
		val errors = new BindException(validator, "errors")
	}

	@Test
	def validateNoCheckpoints() { new Fixture {
		validator.attendanceMonitoringService.countCheckpointsForPoint(point) returns (0)
		validator.validate(errors)
		errors.getAllErrors.size should be (0)
	}}

	@Test
	def validateHasCheckpoints() { new Fixture {
		validator.attendanceMonitoringService.countCheckpointsForPoint(point) returns (2)
		validator.validate(errors)
		errors.getAllErrors.size should be (1)
	}}

}