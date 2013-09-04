package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.util.AutoPopulatingList

class EditMonitoringPointCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends TermServiceComponent 
		with EditMonitoringPointValidation with EditMonitoringPointState {

		val termService = mock[TermService]
		
	}

	trait Fixture {
		val dept = mock[Department]
		val monitoringPoint = new MonitoringPoint
		val existingName = "Point 1"
		val existingWeek = 1
		monitoringPoint.name = existingName
		monitoringPoint.week = existingWeek
		val points = new AutoPopulatingList(classOf[MonitoringPoint])
		points.add(monitoringPoint)
		val pointIndex = 0
		val command = new EditMonitoringPointCommand(dept, pointIndex) with CommandTestSupport
		command.monitoringPoints = points
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
			val newPoint = new MonitoringPoint
			newPoint.name = existingName
			newPoint.week = existingWeek
			command.name = existingName
			command.week = existingWeek
			command.monitoringPoints.add(newPoint)
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

}
