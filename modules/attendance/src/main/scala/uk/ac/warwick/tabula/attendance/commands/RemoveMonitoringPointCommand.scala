package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}
import scala.collection.JavaConverters._

object RemoveMonitoringPointCommand {
	def apply(set: MonitoringPointSet, point: MonitoringPoint) =
		new RemoveMonitoringPointCommand(set, point)
		with ComposableCommand[MonitoringPoint]
		with RemoveMonitoringPointValidation
		with RemoveMonitoringPointDescription
		with RemoveMonitoringPointPermission
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
}

/**
 * Deletes an existing monitoring point.
 */
abstract class RemoveMonitoringPointCommand(val set: MonitoringPointSet, val point: MonitoringPoint)
	extends CommandInternal[MonitoringPoint] with RemoveMonitoringPointState {

	override def applyInternal() = {
		set.remove(point)
		point
	}
}

trait RemoveMonitoringPointValidation extends SelfValidating {
	self: RemoveMonitoringPointState with MonitoringPointServiceComponent =>

	override def validate(errors: Errors) {
		if (set.sentToAcademicOffice) {
			errors.reject("monitoringPointSet.sentToAcademicOffice.points.remove")
		} else if (monitoringPointService.countCheckpointsForPoint(point) > 0) {
			errors.reject("monitoringPoint.hasCheckpoints.remove")
		}

		if (!confirm) errors.rejectValue("confirm", "monitoringPoint.delete.confirm")
	}
}

trait RemoveMonitoringPointPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: RemoveMonitoringPointState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, mandatory(set.route))
	}
}

trait RemoveMonitoringPointDescription extends Describable[MonitoringPoint] {
	self: RemoveMonitoringPointState =>

	override lazy val eventName = "RemoveMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSet(set)
		d.monitoringPoint(point)
	}
}

trait RemoveMonitoringPointState extends GroupMonitoringPointsByTerm with CanPointBeChanged {
	def set: MonitoringPointSet
	def point: MonitoringPoint
	var confirm: Boolean = _

	def monitoringPointsByTerm = groupByTerm(set.points.asScala, set.academicYear)
}

