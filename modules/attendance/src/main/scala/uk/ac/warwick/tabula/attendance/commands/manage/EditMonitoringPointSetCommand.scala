package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.attendance.commands.GroupMonitoringPointsByTerm

object EditMonitoringPointSetCommand {
	def apply(set: MonitoringPointSet) =
		new EditMonitoringPointSetCommand(set)
		with ComposableCommand[MonitoringPointSet]
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with EditMonitoringPointSetPermissions
		with ReadOnly with Unaudited
}


abstract class EditMonitoringPointSetCommand(val set: MonitoringPointSet) extends CommandInternal[MonitoringPointSet]
	with EditMonitoringPointSetState {

	override def applyInternal() = {
		set
	}
}

trait EditMonitoringPointSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditMonitoringPointSetState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, mandatory(set))
	}
}

trait EditMonitoringPointSetState extends GroupMonitoringPointsByTerm with MonitoringPointServiceComponent with CanPointBeChanged {

	def set: MonitoringPointSet

	def monitoringPointsByTerm = groupByTerm(set.points.asScala, set.academicYear)

}
