package uk.ac.warwick.tabula.commands.attendance.old

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSetTemplate
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object ViewMonitoringPointSetTemplateCommand {
	def apply(set: MonitoringPointSetTemplate) = new ViewMonitoringPointSetTemplateCommand(set)
		with ComposableCommand[MonitoringPointSetTemplate]
		with Unaudited
		with ViewMonitoringPointSetTemplatePermissions
		with ViewMonitoringPointSetTemplateState
		with AutowiringTermServiceComponent
}

/**
 * Simply returns a point set as passed in, which would be pointless if it weren't
 * for the permissions checks we do in the accompanying trait.
 */
abstract class ViewMonitoringPointSetTemplateCommand(val set: MonitoringPointSetTemplate)
	extends CommandInternal[MonitoringPointSetTemplate] with ViewMonitoringPointSetTemplateState {
	def applyInternal = set
}

trait ViewMonitoringPointSetTemplateState extends GroupMonitoringPointsByTerm {
	def set: MonitoringPointSetTemplate

	var academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

	// Just used to access week render setting
	var department: Department = null

	def academicYearToUse = academicYear

	def monitoringPointsByTerm = groupByTerm(set.points.asScala, academicYearToUse)
}

trait ViewMonitoringPointSetTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMonitoringPointSetTemplateState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPointTemplates.View)
	}

}
