package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointReport, MonitoringCheckpoint, MonitoringPointType, MonitoringPointSet, MonitoringPoint}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{Assignment, Module, StudentRelationshipType, Department, Route}
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.util.termdates.Term
import uk.ac.warwick.tabula.attendance.commands.manage.old.{UpdateMonitoringPointState, UpdateMonitoringPointValidation, UpdateMonitoringPointCommand}

class UpdateMonitoringPointCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends TermServiceComponent with MonitoringPointServiceComponent with SmallGroupServiceComponent
		with ModuleAndDepartmentServiceComponent with UpdateMonitoringPointValidation with UpdateMonitoringPointState {
		val monitoringPointService = mock[MonitoringPointService]
		val termService = mock[TermService]
		val smallGroupService = mock[SmallGroupService]
		val moduleAndDepartmentService = mock[ModuleAndDepartmentService]
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
		term.getTermTypeAsString returns "Autumn"
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
			// TAB-2025
			there was no(command.termService).getTermFromAcademicWeek(any[Int], any[AcademicYear], any[Boolean])
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
	def validateHasCheckpointsAllowChanges() {
		new Fixture {
			command.monitoringPointService.countCheckpointsForPoint(monitoringPoint) returns 2
			command.name = "New name"
			command.validFromWeek = existingWeek
			command.requiredFromWeek = existingWeek
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = false)
		}
	}

	@Test
	def validateHasCheckpointsValidFromLater() {
		new Fixture {
			command.monitoringPointService.countCheckpointsForPoint(monitoringPoint) returns 2
			command.name = "New name"
			command.validFromWeek = existingWeek + 1
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
	def validateValidSmallGroupAnyModules() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.SmallGroup
			command.smallGroupEventQuantity = 1
			command.isAnySmallGroupEventModules = true
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = false)
		}
	}

	@Test
	def validateValidSmallGroupSpecificModules() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.SmallGroup
			command.smallGroupEventQuantity = 1
			command.isAnySmallGroupEventModules = false
			command.smallGroupEventModules = JSet(new Module, new Module)
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = false)
		}
	}

	@Test
	def validateSmallGroupZeroQuantity() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.SmallGroup
			command.smallGroupEventQuantity = 0
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("smallGroupEventQuantity") should not be null
		}
	}

	@Test
	def validateSmallGroupNoModules() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.SmallGroup
			command.smallGroupEventQuantity = 1
			command.isAnySmallGroupEventModules = false
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("smallGroupEventModules") should not be null
		}
	}

	@Test
	def validateValidSubmissionSpecificModules() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.AssignmentSubmission
			command.assignmentSubmissionQuantity = 1
			command.isAssignmentSubmissionDisjunction = false
			command.assignmentSubmissionModules = JSet(new Module, new Module)
			command.isSpecificAssignments = false
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = false)
		}
	}

	@Test
	def validateSpecificModulesSubmissionZeroQuantity() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.AssignmentSubmission
			command.assignmentSubmissionQuantity = 0
			command.isAssignmentSubmissionDisjunction = false
			command.assignmentSubmissionModules = JSet(new Module, new Module)
			command.isSpecificAssignments = false
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("assignmentSubmissionQuantity") should not be null
		}
	}

	@Test
	def validateSpecificModulesEmpty() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.AssignmentSubmission
			command.assignmentSubmissionQuantity = 1
			command.isAssignmentSubmissionDisjunction = false
			command.assignmentSubmissionModules = JSet()
			command.isSpecificAssignments = false
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldErrors.size should be(1)
			errors.getFieldError("assignmentSubmissionModules") should not be null
		}
	}

	@Test
	def validateValidSpecificAssignments() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.AssignmentSubmission
			command.assignmentSubmissionQuantity = 1
			command.isAssignmentSubmissionDisjunction = false
			command.isSpecificAssignments = true
			command.assignmentSubmissionAssignments = JSet(new Assignment)
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = false)
		}
	}

	@Test
	def validateSpecificAssignmentsEmpty() {
		new Fixture {
			command.name = "Name"
			command.validFromWeek = 1
			command.requiredFromWeek = 1
			command.pointType = MonitoringPointType.AssignmentSubmission
			command.assignmentSubmissionQuantity = 1
			command.isAssignmentSubmissionDisjunction = false
			command.isSpecificAssignments = true
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
			errors.getFieldError("assignmentSubmissionAssignments") should not be null
		}
	}

	@Test
	def validateAlreadyReportedThisTerm() {
		new Fixture {
			command.name = "New name"
			command.validFromWeek = existingWeek
			command.requiredFromWeek = existingWeek

			val student = Fixtures.student("12345")

			command.termService.getTermFromAcademicWeekIncludingVacations(monitoringPoint.validFromWeek, set.academicYear) returns term
			command.termService.getTermFromAcademicWeekIncludingVacations(otherMonitoringPoint.validFromWeek, set.academicYear) returns term
			command.monitoringPointService.getCheckpointsByStudent(set.points.asScala) returns Seq((student, mock[MonitoringCheckpoint]))
			// there is already a report sent for this term, so we cannot edit this Monitoring Point
			command.monitoringPointService.findReports(Seq(student), set.academicYear, term.getTermTypeAsString ) returns Seq(new MonitoringPointReport)

			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = true)
		}
	}

}
