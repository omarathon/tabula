package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointReport, MonitoringCheckpoint, MonitoringPointSet, MonitoringPoint}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.attendance.commands.manage.{RemoveMonitoringPointState, RemoveMonitoringPointValidation, RemoveMonitoringPointCommand}
import scala.collection.JavaConverters._
import uk.ac.warwick.util.termdates.Term

class RemoveMonitoringPointCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends TermServiceComponent with MonitoringPointServiceComponent
		with RemoveMonitoringPointValidation with RemoveMonitoringPointState {
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
		monitoringPoint.validFromWeek = existingWeek
		monitoringPoint.requiredFromWeek = existingWeek
		monitoringPoint.pointSet = set
		val otherMonitoringPoint = new MonitoringPoint
		val otherExistingName = "Point 2"
		val otherExistingWeek = 2
		otherMonitoringPoint.name = otherExistingName
		otherMonitoringPoint.validFromWeek = otherExistingWeek
		otherMonitoringPoint.requiredFromWeek = otherExistingWeek
		set.points = JArrayList(monitoringPoint, otherMonitoringPoint)
		val command = new RemoveMonitoringPointCommand(set, monitoringPoint) with CommandTestSupport
		command.monitoringPointService.getCheckpointsByStudent(set.points.asScala) returns Seq.empty
		val term = mock[Term]
		term.getTermTypeAsString() returns ("Autumn")
	}

	@Test
	def validateNoConfirm() {
		new Fixture {
			command.confirm = false
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasFieldErrors should be (right = true)
		}
	}

	@Test
	def validateHasCheckpointsNoChanges() {
		new Fixture {
			command.monitoringPointService.countCheckpointsForPoint(monitoringPoint) returns 2
			command.confirm = true
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = true)
		}
	}

	@Test
	def validateAlreadyReportedThisTerm() {
		new Fixture {
			monitoringPoint.sentToAcademicOffice = true
			command.confirm = true

			val student = Fixtures.student("12345")

			command.termService.getTermFromAcademicWeek(monitoringPoint.validFromWeek, set.academicYear) returns (term)
			command.termService.getTermFromAcademicWeek(otherMonitoringPoint.validFromWeek, set.academicYear) returns (term)
			command.monitoringPointService.getCheckpointsByStudent(set.points.asScala) returns Seq((student, mock[MonitoringCheckpoint]))
			// there is already a report sent for this term, so we cannot remove this Monitoring Point
			command.monitoringPointService.findReports(Seq(student), set.academicYear, term.getTermTypeAsString ) returns Seq(new MonitoringPointReport)

			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = true)
		}
	}

}
