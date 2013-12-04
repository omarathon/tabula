package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.attendance.commands.manage.{RemoveMonitoringPointState, RemoveMonitoringPointValidation, RemoveMonitoringPointCommand}
import scala.collection.JavaConverters._

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
	def validateSentToAcademicOfficeNoChanges() {
		new Fixture {
			monitoringPoint.sentToAcademicOffice = true
			command.confirm = true
			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = true)
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

}
