package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.JavaImports.JArrayList

class SetMonitoringPointsCommandTest extends TestBase with Mockito {

	trait Fixture {
		val set = new MonitoringPointSet
		set.route = mock[Route]
		val monitoringPoint = new MonitoringPoint
		monitoringPoint.id = "1"
		val existingName = "Point 1"
		val existingWeek = 1
		monitoringPoint.name = existingName
		monitoringPoint.week = existingWeek
		val otherMonitoringPoint = new MonitoringPoint
		otherMonitoringPoint.id = "2"
		val otherExistingName = "Point 2"
		val otherExistingWeek = 2
		otherMonitoringPoint.name = otherExistingName
		otherMonitoringPoint.week = otherExistingWeek
		set.points = JArrayList(monitoringPoint, otherMonitoringPoint)
	}


	@Test
	def validateValid() = withUser("cuslat") {
		new Fixture {
			monitoringPoint.sentToAcademicOffice = false
			val command = SetMonitoringCheckpointCommand(monitoringPoint, currentUser)

			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = false)
		}
	}


	@Test
	def validateSentToAcademicOfficeNoChanges() = withUser("cuslat") {
		new Fixture {
			monitoringPoint.sentToAcademicOffice = true
			val command = SetMonitoringCheckpointCommand(monitoringPoint, currentUser )

			var errors = new BindException(command, "command")
			command.validate(errors)
			errors.hasErrors should be (right = true)
		}
	}

}
