package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointType, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.data.model.Department
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.CheckablePermission


object AddMonitoringPointCommand {
	def apply(dept: Department) =
		new AddMonitoringPointCommand(dept)
			with ComposableCommand[Unit]
			with AutowiringTermServiceComponent
			with AddMonitoringPointValidation
			with AddMonitoringPointPermissions
			with ReadOnly with Unaudited
}

/**
 * Adds a new monitoring point to the set of points in the command's state.
 * Does not persist the change (no monitoring point set yet exists)
 */
abstract class AddMonitoringPointCommand(val dept: Department) extends CommandInternal[Unit] with MonitoringPointState {

	override def applyInternal() = {
		val point = new MonitoringPoint
		copyTo(point)
		monitoringPoints.add(point)
	}
}

trait AddMonitoringPointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: MonitoringPointState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.MonitoringPoints.Manage, mandatory(dept))) ++
			dept.routes.asScala.map { route => CheckablePermission(Permissions.MonitoringPoints.Manage, route) }
		)
	}
}

trait AddMonitoringPointValidation extends SelfValidating with MonitoringPointValidation {
	self: MonitoringPointState =>

	override def validate(errors: Errors) {
		validateWeek(errors, validFromWeek, "validFromWeek")
		validateWeek(errors, requiredFromWeek, "requiredFromWeek")
		validateWeeks(errors, validFromWeek, requiredFromWeek, "validFromWeek")
		validateName(errors, name, "name")

		pointType match {
			case MonitoringPointType.Meeting =>
				validateTypeMeeting(errors,
					meetingRelationships.asScala, "meetingRelationships",
					meetingFormats.asScala, "meetingFormats",
					meetingQuantity, "meetingQuantity",
					dept
				)
			case _ =>
		}

		if (monitoringPoints.asScala.count(p =>
			p.name == name && p.validFromWeek == validFromWeek && p.requiredFromWeek == requiredFromWeek
		) > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("validFromWeek", "monitoringPoint.name.exists")
		}
	}
}

