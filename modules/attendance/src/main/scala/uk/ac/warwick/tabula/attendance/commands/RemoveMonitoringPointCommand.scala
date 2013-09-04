package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}

object RemoveMonitoringPointCommand {
	def apply(set: MonitoringPointSet, point: MonitoringPoint) =
		new RemoveMonitoringPointCommand(set, point)
			with ComposableCommand[Unit]
			with RemoveMonitoringPointValidation
			with RemoveMonitoringPointPermissions
			with RemoveMonitoringPointState
			with RemoveMonitoringPointDescription
			with AutowiringMonitoringPointServiceComponent
}

/**
 * Creates a new monitoring point in a set.
 */
class RemoveMonitoringPointCommand(val set: MonitoringPointSet, val point: MonitoringPoint) extends CommandInternal[Unit] {
	self: RemoveMonitoringPointState with MonitoringPointServiceComponent =>

	override def applyInternal() = {
		monitoringPointService.delete(point)
	}
}

trait RemoveMonitoringPointPermissions extends RequiresPermissionsChecking {
	self: RemoveMonitoringPointState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, set.route)
	}
}

trait RemoveMonitoringPointValidation extends SelfValidating {
	self: RemoveMonitoringPointState =>

	override def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "monitoringPoint.delete.confirm")
	}
}

trait RemoveMonitoringPointDescription extends Describable[Unit] {
	self: RemoveMonitoringPointState =>

	override lazy val eventName = "RemoveMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSet(set)
		d.property("name", point.name)
	}
}

trait RemoveMonitoringPointState {
	val set: MonitoringPointSet
	val point: MonitoringPoint
	var confirm: Boolean = _
}