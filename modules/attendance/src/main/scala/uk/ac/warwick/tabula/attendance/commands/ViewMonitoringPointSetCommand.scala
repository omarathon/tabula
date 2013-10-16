package uk.ac.warwick.tabula.attendance.commands

import scala.collection.JavaConverters._

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSetTemplate, MonitoringPointSet, AbstractMonitoringPointSet}
import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, RequiresPermissionsChecking, PermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.data.model.Department

object ViewMonitoringPointSetCommand {
	def apply(set: AbstractMonitoringPointSet) = new ViewMonitoringPointSetCommand(set)
		with ComposableCommand[AbstractMonitoringPointSet]
		with Unaudited
		with ViewMonitoringPointSetPermissions
		with ViewMonitoringPointSetState
		with AutowiringTermServiceComponent
}

/**
 * Simply returns a point set as passed in, which would be pointless if it weren't
 * for the permissions checks we do in the accompanying trait.
 */
abstract class ViewMonitoringPointSetCommand(val set: AbstractMonitoringPointSet)
	extends CommandInternal[AbstractMonitoringPointSet] with ViewMonitoringPointSetState {
	def applyInternal = set
}

trait ViewMonitoringPointSetState extends GroupMonitoringPointsByTerm {
	def set: AbstractMonitoringPointSet

	var academicYear: AcademicYear = AcademicYear.guessByDate(DateTime.now)

	// Just used to access week render setting
	var department: Department = set match {
		case s:MonitoringPointSet => s.route.department
		case _ => null
	}

	def academicYearToUse = set match {
		case s:MonitoringPointSet => s.academicYear
		case _ => academicYear
	}

	def monitoringPointsByTerm = groupByTerm(set.points.asScala, academicYearToUse)
}

trait ViewMonitoringPointSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMonitoringPointSetState =>

	def permissionsCheck(p: PermissionsChecking) {
		mandatory(set) match {
			case s: MonitoringPointSet => p.PermissionCheck(Permissions.MonitoringPoints.View, s)
			case s: MonitoringPointSetTemplate => p.PermissionCheck(Permissions.MonitoringPointSetTemplates.View)
		}
	}

}
