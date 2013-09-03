package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.data.model.Department
import scala.collection.JavaConverters._
import org.springframework.util.AutoPopulatingList
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions

object DeleteMonitoringPointCommand {
	def apply(dept: Department, pointIndex: Int) =
		new DeleteMonitoringPointCommand(dept, pointIndex)
		with ComposableCommand[Unit]
		with AutowiringTermServiceComponent
		with DeleteMonitoringPointValidation
		with DeleteMonitoringPointPermissions
		with ReadOnly with Unaudited
}

/**
 * Deletes an existing monitoring point from the set of points in the command's state.
 * Does not persist the change (no monitoring point set yet exists)
 */
abstract class DeleteMonitoringPointCommand(val dept: Department, val pointIndex: Int)
	extends CommandInternal[Unit] with DeleteMonitoringPointState {

	override def applyInternal() = {
		monitoringPoints.remove(pointIndex)
	}
}

trait DeleteMonitoringPointValidation extends SelfValidating {
	self: DeleteMonitoringPointState =>

	override def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "monitoringPoint.delete.confirm")
	}
}

trait DeleteMonitoringPointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DeleteMonitoringPointState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, mandatory(dept))
	}
}

trait DeleteMonitoringPointState extends GroupMonitoringPointsByTerm {
	val dept: Department
	val pointIndex: Int
	var confirm: Boolean = _
	var monitoringPoints = new AutoPopulatingList(classOf[MonitoringPoint])
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())
	def monitoringPointsByTerm = groupByTerm(monitoringPoints.asScala, academicYear)
}

