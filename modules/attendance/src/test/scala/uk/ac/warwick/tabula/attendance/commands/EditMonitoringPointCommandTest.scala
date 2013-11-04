package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointType, MonitoringPoint}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}
import org.springframework.util.AutoPopulatingList
import uk.ac.warwick.tabula.JavaImports.JHashSet
import uk.ac.warwick.tabula.attendance.commands.manage.{EditMonitoringPointState, EditMonitoringPointValidation, EditMonitoringPointCommand}

class EditMonitoringPointCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends TermServiceComponent
		with EditMonitoringPointValidation with EditMonitoringPointState {

		val termService = mock[TermService]

	}

	trait Fixture {
		val dept = new Department
		dept.relationshipService = mock[RelationshipService]
		val monitoringPoint = new MonitoringPoint
		val existingName = "Point 1"
		val existingWeek = 5
		monitoringPoint.name = existingName
		monitoringPoint.validFromWeek = existingWeek
		monitoringPoint.requiredFromWeek = existingWeek
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
			val newPoint = new MonitoringPoint
			newPoint.name = existingName
			newPoint.validFromWeek = existingWeek
			newPoint.requiredFromWeek = existingWeek
			command.name = existingName
			command.validFromWeek = existingWeek
			command.requiredFromWeek = existingWeek
			command.monitoringPoints.add(newPoint)
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
	def validateWrongWeekOrder() {
		new Fixture {
			command.validFromWeek = 20
			command.requiredFromWeek = 10
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("validFromWeek") should not be null
		}
	}

	@Test
	def validateMeetingNoRelationshipsNoFormats() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.Meeting
			command.meetingRelationships = JHashSet()
			command.meetingFormats = JHashSet()
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("meetingRelationships") should not be null
			errors.getFieldError("meetingFormats") should not be null
		}
	}

	@Test
	def validateMeetingInvalidRelationships() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.Meeting
			dept.relationshipService.allStudentRelationshipTypes returns Seq()
			command.meetingRelationships = JHashSet(
				StudentRelationshipType("Valid", "Valid", "Valid", "Valid"),
				StudentRelationshipType("Invalid", "Invalid", "Invalid", "Invalid"))
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("meetingRelationships") should not be null
		}
	}

	@Test
	def validateMeetingZeroQuantity() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.Meeting
			command.meetingQuantity = 0
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("meetingQuantity") should not be null
		}
	}

}
