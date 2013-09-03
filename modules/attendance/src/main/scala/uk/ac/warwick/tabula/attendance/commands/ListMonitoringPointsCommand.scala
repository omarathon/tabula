package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}


object ListMonitoringPointsCommand {
	def apply(page: Int) =
		new ListMonitoringPointsCommand(page)
			with ComposableCommand[Seq[MonitoringPoint]]
			with ListMonitoringPointsCommandPermissions
			with AutowiringMonitoringPointServiceComponent
			with ReadOnly
			with Unaudited
}

abstract class ListMonitoringPointsCommand(val page: Int)
	extends CommandInternal[Seq[MonitoringPoint]] with Appliable[Seq[MonitoringPoint]]
	with ListMonitoringPointsCommandState {
	 self: MonitoringPointServiceComponent =>

	def applyInternal(): Seq[MonitoringPoint] = {
		monitoringPointService.list(page)
	}
}


trait ListMonitoringPointsCommandPermissions extends RequiresPermissionsChecking {
	self: ListMonitoringPointsCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		val q = p.PermissionCheck(Permissions.MonitoringPoints.View)
		q
	}
}

trait ListMonitoringPointsCommandState {
	val page: Int
}
