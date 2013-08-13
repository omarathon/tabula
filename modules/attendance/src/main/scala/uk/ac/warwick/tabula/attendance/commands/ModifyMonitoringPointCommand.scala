package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.commands.CommandInternal
import org.joda.time.DateTime

abstract class ModifyMonitoringPointCommand extends CommandInternal[MonitoringPoint] with ModifyMonitoringPointState {
	def copyTo(point: MonitoringPoint) {
		point.name = this.name
		point.defaultValue = this.defaultValue
		point.week = this.week
		point.pointSet = this.set
		point.updatedDate = new DateTime()
	}

	def copyFrom(point: MonitoringPoint) {
		this.name = point.name
		this.defaultValue = point.defaultValue
		this.week = point.week
	}
}

trait ModifyMonitoringPointPermissions extends RequiresPermissionsChecking {
	self: ModifyMonitoringPointState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, set.route)
	}
}

trait ModifyMonitoringPointState {
	val set: MonitoringPointSet

	var name: String = _
	var defaultValue: Boolean = _
	var week: Int = 0
}