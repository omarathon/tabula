package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.Route
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
		monitoringPoint.id = "1"
		val existingName = "Point 1"
		val existingWeek = 5
		monitoringPoint.name = existingName
		monitoringPoint.validFromWeek = existingWeek
		monitoringPoint.requiredFromWeek = existingWeek
		val otherMonitoringPoint = new MonitoringPoint
		otherMonitoringPoint.id = "2"
		val otherExistingName = "Point 2"
		val otherExistingWeek = 6
		otherMonitoringPoint.name = otherExistingName
		otherMonitoringPoint.validFromWeek = otherExistingWeek
		otherMonitoringPoint.requiredFromWeek = otherExistingWeek
		set.points = JArrayList(monitoringPoint, otherMonitoringPoint)
		val command = new UpdateMonitoringPointCommand(set, monitoringPoint) with CommandTestSupport
	}

	@Test
	def validateValid() {
		new Fixture {
			command.name = "New name"
			command.validFromWeek = existingWeek
			command.requiredFromWeek = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = false)
		}
	}

	@Test
	def validateAlsoValid() {
		new Fixture {
			command.name = existingName
			command.validFromWeek = 2
			command.requiredFromWeek = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = false)
		}
	}

	@Test
	def validateAlsoAlsoValid() {
		new Fixture {
			command.name = existingName
			command.validFromWeek = existingWeek
			command.requiredFromWeek = 10
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = false)
		}
	}

	@Test
	def validateSamePoint() {
		new Fixture {
			command.name = existingName
			command.validFromWeek = existingWeek
			command.requiredFromWeek = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = false)
		}
	}

	@Test
	def validateDuplicatePoint() {
		new Fixture {
			command.name = otherExistingName
			command.validFromWeek = otherExistingWeek
			command.requiredFromWeek = otherExistingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("name") should not be null
			errors.getFieldError("validFromWeek") should not be null
		}
	}

	@Test
	def validateInvalidValidFromWeek() {
		new Fixture {
			command.name = existingName
			command.validFromWeek = 53
			command.requiredFromWeek = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("validFromWeek") should not be null
		}
	}

	@Test
	def validateInvalidRequiredFromWeek() {
		new Fixture {
			command.name = existingName
			command.validFromWeek = existingWeek
			command.requiredFromWeek = 53
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("requiredFromWeek") should not be null
		}
	}

	@Test
	def validateMissingName() {
		new Fixture {
			command.name = ""
			command.validFromWeek = existingWeek
			command.requiredFromWeek = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("name") should not be null
		}
	}

	@Test
	def validateSentToAcademicOfficeNoChanges() {
		new Fixture {
			monitoringPoint.sentToAcademicOffice = true
			command.name = "New name"
			command.validFromWeek = existingWeek
			command.requiredFromWeek = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = true)
		}
	}

	@Test
	def validateHasCheckpointsNoChanges() {
		new Fixture {
			command.monitoringPointService.countCheckpointsForPoint(monitoringPoint) returns 2
			command.name = "New name"
			command.validFromWeek = existingWeek
			command.requiredFromWeek = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = true)
		}
	}

}
