package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointReport, MonitoringCheckpoint, MonitoringPointType, MonitoringPointSet, MonitoringPoint}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department, Route}
import uk.ac.warwick.tabula.JavaImports.{JHashSet, JArrayList}
import uk.ac.warwick.tabula.attendance.commands.manage.{UpdateMonitoringPointState, UpdateMonitoringPointValidation, UpdateMonitoringPointCommand}
import scala.collection.JavaConverters._
import uk.ac.warwick.util.termdates.Term

class UpdateMonitoringPointCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends TermServiceComponent with MonitoringPointServiceComponent
		with UpdateMonitoringPointValidation with UpdateMonitoringPointState {
		val monitoringPointService = mock[MonitoringPointService]
		val termService = mock[TermService]
	}

	trait Fixture {
		val set = new MonitoringPointSet
		set.route = new Route
		val dept = new Department
		dept.relationshipService = mock[RelationshipService]
		set.route.department = dept
		val monitoringPoint = new MonitoringPoint
		monitoringPoint.id = "1"
		val existingName = "Point 1"
		val existingWeek = 5
		monitoringPoint.name = existingName
		monitoringPoint.validFromWeek = existingWeek
		monitoringPoint.requiredFromWeek = existingWeek
		monitoringPoint.pointSet = set
		val otherMonitoringPoint = new MonitoringPoint
		otherMonitoringPoint.id = "2"
		val otherExistingName = "Point 2"
		val otherExistingWeek = 6
		otherMonitoringPoint.name = otherExistingName
		otherMonitoringPoint.validFromWeek = otherExistingWeek
		otherMonitoringPoint.requiredFromWeek = otherExistingWeek
		set.points = JArrayList(monitoringPoint, otherMonitoringPoint)
		val command = new UpdateMonitoringPointCommand(set, monitoringPoint) with CommandTestSupport
		command.monitoringPointService.getCheckpointsByStudent(set.points.asScala) returns Seq.empty
		val term = mock[Term]
		term.getTermTypeAsString() returns ("Autumn")
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

	@Test
	def validateAlreadyReportedThisTerm() {
		new Fixture {
			command.name = "New name"
			command.validFromWeek = existingWeek
			command.requiredFromWeek = existingWeek

			val student = Fixtures.student("12345")

			command.termService.getTermFromAcademicWeek(monitoringPoint.validFromWeek, set.academicYear) returns (term)
			command.termService.getTermFromAcademicWeek(otherMonitoringPoint.validFromWeek, set.academicYear) returns (term)
			command.monitoringPointService.getCheckpointsByStudent(set.points.asScala) returns Seq((student, mock[MonitoringCheckpoint]))
			// there is already a report sent for this term, so we cannot edit this Monitoring Point
			command.monitoringPointService.findReports(Seq(student), set.academicYear, term.getTermTypeAsString ) returns Seq(new MonitoringPointReport)

			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = true)
		}
	}

}
