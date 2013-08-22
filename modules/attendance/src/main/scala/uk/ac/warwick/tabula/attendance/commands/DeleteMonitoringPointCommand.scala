package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{AutowiringRouteServiceComponent, RouteServiceComponent}

object DeleteMonitoringPointCommand {
	def apply(set: MonitoringPointSet, point: MonitoringPoint) =
		new DeleteMonitoringPointCommand(set, point)
			with ComposableCommand[Unit]
			with DeleteMonitoringPointValidation
			with DeleteMonitoringPointPermissions
			with DeleteMonitoringPointState
			with DeleteMonitoringPointDescription
			with AutowiringRouteServiceComponent
}

/**
 * Creates a new monitoring point in a set.
 */
class DeleteMonitoringPointCommand(val set: MonitoringPointSet, val point: MonitoringPoint) extends CommandInternal[Unit] {
	self: DeleteMonitoringPointState with RouteServiceComponent =>

	override def applyInternal() = {
		routeService.delete(point)
	}
}

trait DeleteMonitoringPointPermissions extends RequiresPermissionsChecking {
	self: DeleteMonitoringPointState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, set.route)
	}
}

trait DeleteMonitoringPointValidation extends SelfValidating {
	self: DeleteMonitoringPointState =>

	override def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "monitoringPoint.delete.confirm")
	}
}

trait DeleteMonitoringPointDescription extends Describable[Unit] {
	self: DeleteMonitoringPointState =>

	override lazy val eventName = "DeleteMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSet(set)
		d.property("name", point.name)
	}
}

trait DeleteMonitoringPointState {
	val set: MonitoringPointSet
	val point: MonitoringPoint
	var confirm: Boolean = _
}