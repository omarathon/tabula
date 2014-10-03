package uk.ac.warwick.tabula.attendance.commands.old

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object ViewMonitoringPointSetCommand {
	def apply(set: MonitoringPointSet) = new ViewMonitoringPointSetCommand(set)
		with ComposableCommand[MonitoringPointSet]
		with Unaudited
		with ViewMonitoringPointSetPermissions
		with ViewMonitoringPointSetState
		with AutowiringTermServiceComponent
}

/**
 * Simply returns a point set as passed in, which would be pointless if it weren't
 * for the permissions checks we do in the accompanying trait.
 */
abstract class ViewMonitoringPointSetCommand(val set: MonitoringPointSet)
	extends CommandInternal[MonitoringPointSet] with ViewMonitoringPointSetState {
	def applyInternal = set
}

trait ViewMonitoringPointSetState extends GroupMonitoringPointsByTerm {
	def set: MonitoringPointSet

	var academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

	// Just used to access week render setting
	var department: Department = set.route.adminDepartment

	def academicYearToUse = set.academicYear

	def monitoringPointsByTerm = groupByTerm(set.points.asScala, academicYearToUse)
}

trait ViewMonitoringPointSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMonitoringPointSetState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, mandatory(set))
	}

}
