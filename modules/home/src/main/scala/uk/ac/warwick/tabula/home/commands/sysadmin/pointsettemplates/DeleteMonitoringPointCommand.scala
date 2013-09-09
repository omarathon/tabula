package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import org.springframework.util.AutoPopulatingList

object DeleteMonitoringPointCommand {
	def apply(pointIndex: Int) =
		new DeleteMonitoringPointCommand(pointIndex)
		with ComposableCommand[Unit]
		with DeleteMonitoringPointValidation
		with MonitoringPointSetTemplatesPermissions
		with ReadOnly with Unaudited
}

/**
 * Deletes an existing monitoring point from the set of points in the command's state.
 * Does not persist the change (no monitoring point set yet exists)
 */
abstract class DeleteMonitoringPointCommand(val pointIndex: Int)
	extends CommandInternal[Unit] with DeleteMonitoringPointState {

	override def applyInternal() = {
		monitoringPoints.remove(pointIndex)
	}
}

trait DeleteMonitoringPointValidation extends SelfValidating {
	self: DeleteMonitoringPointState =>

	override def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "monitoringPoint.delete.confirm")
	}
}

trait DeleteMonitoringPointState {
	val pointIndex: Int
	var confirm: Boolean = _
	var monitoringPoints = new AutoPopulatingList(classOf[MonitoringPoint])
}

