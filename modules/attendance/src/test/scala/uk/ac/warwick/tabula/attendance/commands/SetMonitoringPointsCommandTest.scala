package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpoint, MonitoringPoint, MonitoringPointSet}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.services.{MonitoringPointService, MonitoringPointServiceComponent, ProfileService, ProfileServiceComponent}

class SetMonitoringPointsCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends ProfileServiceComponent with MonitoringPointServiceComponent
		with SetMonitoringCheckpointCommandValidation with SetMonitoringCheckpointState {
		val profileService = mock[ProfileService]
		val monitoringPointService = mock[MonitoringPointService]

		def apply(): Seq[MonitoringCheckpoint] = {
			null
		}
	}

	trait Fixture {
		val set = new MonitoringPointSet
		set.route = mock[Route]
		val monitoringPoint = new MonitoringPoint
		monitoringPoint.id = "1"
		val existingName = "Point 1"
		val existingWeek = 1
		monitoringPoint.name = existingName
		monitoringPoint.validFromWeek = existingWeek
		monitoringPoint.requiredFromWeek = existingWeek
		val otherMonitoringPoint = new MonitoringPoint
		otherMonitoringPoint.id = "2"
		val otherExistingName = "Point 2"
		val otherExistingWeek = 2
		otherMonitoringPoint.name = otherExistingName
		otherMonitoringPoint.validFromWeek = otherExistingWeek
		otherMonitoringPoint.requiredFromWeek = otherExistingWeek
		set.points = JArrayList(monitoringPoint, otherMonitoringPoint)
	}


	@Test
	def validateValid() = withUser("cuslat") {
		new Fixture {
			monitoringPoint.sentToAcademicOffice = false
			val command = new SetMonitoringCheckpointCommand(monitoringPoint, currentUser) with CommandTestSupport

			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = false)
		}
	}


	@Test
	def validateSentToAcademicOfficeNoChanges() = withUser("cuslat") {
		new Fixture {
			monitoringPoint.sentToAcademicOffice = true
			val command = new SetMonitoringCheckpointCommand(monitoringPoint, currentUser) with CommandTestSupport

			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = true)
		}
	}

}
