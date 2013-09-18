package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.services.{MonitoringPointServiceComponent, AutowiringMonitoringPointServiceComponent}
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSetTemplate

object ListMonitoringPointSetTemplatesCommand {
	def apply() =
		new ListMonitoringPointSetTemplatesCommandInternal
			with ComposableCommand[Seq[MonitoringPointSetTemplate]]
			with AutowiringMonitoringPointServiceComponent
			with ListMonitoringPointSetTemplatesCommandPermissions
			with Unaudited
}

class ListMonitoringPointSetTemplatesCommandInternal extends CommandInternal[Seq[MonitoringPointSetTemplate]] {
	this: MonitoringPointServiceComponent =>

	override def applyInternal() = monitoringPointService.listTemplates
}

trait ListMonitoringPointSetTemplatesCommandPermissions extends RequiresPermissionsChecking {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPointSetTemplates.Manage)
	}
}