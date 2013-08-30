package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports.JArrayList

class ModifyMonitoringPointCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends RouteServiceComponent
			with ModifyMonitoringPointValidation with ModifyMonitoringPointState {
		val routeService = mock[RouteService]

		def apply(): MonitoringPoint = {
			null
		}
	}

	trait Fixture {
		val set = new MonitoringPointSet
		val monitoringPoint = new MonitoringPoint
		val existingName = "Point 1"
		val existingWeek = 1
		monitoringPoint.id = "1"
		monitoringPoint.name = existingName
		monitoringPoint.week = existingWeek
		val points = JArrayList(monitoringPoint)
		set.points = points
		val command = new ModifyMonitoringPointCommand(set, monitoringPoint) with CommandTestSupport
	}

	@Test
	def validateValid() {
		new Fixture {
			command.name = "New name"
			command.week = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors() should be (false)
		}
	}

	@Test
	def validateAlsoValid() {
		new Fixture {
			command.name = existingName
			command.week = 2
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors() should be (false)
		}
	}

	@Test
	def validateSamePoint() {
		new Fixture {
			command.name = existingName
			command.week = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors() should be (false)
		}
	}

	@Test
	def validateDuplicatePoint() {
		new Fixture {
			val newPoint = new MonitoringPoint
			newPoint.id = "2"
			command.name = existingName
			command.week = existingWeek
			command.point = newPoint
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors() should be (true)
			errors.getFieldError("name") should not be (null)
			errors.getFieldError("week") should not be (null)
		}
	}

	@Test
	def validateInvalidWeek() {
		new Fixture {
			command.name = existingName
			command.week = 53
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors() should be (true)
			errors.getFieldError("week") should not be (null)
		}
	}

	@Test
	def validateMissingName() {
		new Fixture {
			command.name = ""
			command.week = 1
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors() should be (true)
			errors.getFieldError("name") should not be (null)
		}
	}

}
