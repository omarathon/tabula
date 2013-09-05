package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{Route, Department}
import org.springframework.util.AutoPopulatingList
import uk.ac.warwick.tabula.JavaImports.JArrayList

class UpdateMonitoringPointCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends TermServiceComponent with MonitoringPointServiceComponent
		with UpdateMonitoringPointValidation with UpdateMonitoringPointState {
		val monitoringPointService = mock[MonitoringPointService]
		val termService = mock[TermService]
	}

	trait Fixture {
		val set = new MonitoringPointSet
		set.route = mock[Route]
		val monitoringPoint = new MonitoringPoint
		val existingName = "Point 1"
		val existingWeek = 1
		monitoringPoint.name = existingName
		monitoringPoint.week = existingWeek
		val otherMonitoringPoint = new MonitoringPoint
		val otherExistingName = "Point 2"
		val otherExistingWeek = 2
		otherMonitoringPoint.name = otherExistingName
		otherMonitoringPoint.week = otherExistingWeek
		set.points = JArrayList(monitoringPoint, otherMonitoringPoint)
		val command = new UpdateMonitoringPointCommand(set, monitoringPoint) with CommandTestSupport
	}

	@Test
	def validateValid() {
		new Fixture {
			command.name = "New name"
			command.week = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (false)
		}
	}

	@Test
	def validateAlsoValid() {
		new Fixture {
			command.name = existingName
			command.week = 2
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (false)
		}
	}

	@Test
	def validateSamePoint() {
		new Fixture {
			command.name = existingName
			command.week = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (false)
		}
	}

	@Test
	def validateDuplicatePoint() {
		new Fixture {
			command.name = otherExistingName
			command.week = otherExistingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (true)
			errors.getFieldError("name") should not be null
			errors.getFieldError("week") should not be null
		}
	}

	@Test
	def validateInvalidWeek() {
		new Fixture {
			command.name = existingName
			command.week = 53
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (true)
			errors.getFieldError("week") should not be null
		}
	}

	@Test
	def validateMissingName() {
		new Fixture {
			command.name = ""
			command.week = 1
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (true)
			errors.getFieldError("name") should not be null
		}
	}

	@Test
	def validateSentToAcademicOfficeNoChanges() {
		new Fixture {
			set.sentToAcademicOffice = true
			command.name = "New name"
			command.week = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (true)
		}
	}

	@Test
	def validateHasCheckpointsNoChanges() {
		new Fixture {
			command.monitoringPointService.countCheckpointsForPoint(monitoringPoint) returns 2
			command.name = "New name"
			command.week = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (true)
		}
	}

}
