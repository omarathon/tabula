package uk.ac.warwick.tabula.scheduling.commands

import uk.ac.warwick.tabula.commands.{Describable, ComposableCommand, CommandInternal, Description, Command}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}

object ExportAttendanceToSitsCommand {
	def apply() = new ExportAttendanceToSitsCommand with ComposableCommand[Unit] with ExportAttendanceToSitsCommandPermissions
		with ExportAttendanceToSitsCommandDescription with AutowiringMonitoringPointServiceComponent
}

class ExportAttendanceToSitsCommand extends CommandInternal[Unit] {

	self: MonitoringPointServiceComponent =>

	override def applyInternal() = {
		// get the reports from MPService
		val unreportedReports = monitoringPointService.findUnreportedReports()


		// group reports by student, with their reports in term order

		// for each, write the stuff to STIS using other service
	}

}

trait ExportAttendanceToSitsCommandPermissions extends RequiresPermissionsChecking {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Export)
	}
}

trait ExportAttendanceToSitsCommandDescription extends Describable[Unit] {
	override def describe(d: Description) {}
}