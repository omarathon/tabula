package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import scala.collection.JavaConverters._
import org.springframework.util.AutoPopulatingList


object AddMonitoringPointCommand {
	def apply() =
		new AddMonitoringPointCommand()
		with ComposableCommand[Unit]
		with AddMonitoringPointValidation
		with MonitoringPointSetTemplatesPermissions
		with ReadOnly with Unaudited
}

/**
 * Adds a new monitoring point to the set of points in the command's state.
 * Does not persist the change (no monitoring point set yet exists)
 */
abstract class AddMonitoringPointCommand() extends CommandInternal[Unit] with AddMonitoringPointState {

	override def applyInternal() = {
		val point = new MonitoringPoint
		point.name = name
		point.validFromWeek = validFromWeek
		point.requiredFromWeek = requiredFromWeek
		monitoringPoints.add(point)
	}
}

trait AddMonitoringPointValidation extends SelfValidating with MonitoringPointValidation {
	self: AddMonitoringPointState =>

	override def validate(errors: Errors) {
		validateWeek(errors, validFromWeek, "validFromWeek")
		validateWeek(errors, requiredFromWeek, "requiredFromWeek")
		validateWeeks(errors, validFromWeek, requiredFromWeek, "validFromWeek")
		validateName(errors, name, "name")

		if (monitoringPoints.asScala.count(p =>
			p.name == name && p.validFromWeek == validFromWeek && p.requiredFromWeek == requiredFromWeek
		) > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("validFromWeek", "monitoringPoint.name.exists")
		}
	}
}

trait AddMonitoringPointState {
	var monitoringPoints = new AutoPopulatingList(classOf[MonitoringPoint])
	var name: String = _
	var validFromWeek: Int = 0
	var requiredFromWeek: Int = 0
}

