package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.data.model.Department
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointType
import uk.ac.warwick.tabula.permissions.CheckablePermission

object EditMonitoringPointCommand {
	def apply(dept: Department, pointIndex: Int) =
		new EditMonitoringPointCommand(dept, pointIndex)
			with ComposableCommand[Unit]
			with EditMonitoringPointPermissions
			with AutowiringTermServiceComponent
			with EditMonitoringPointValidation
			with ReadOnly with Unaudited
}

/**
 * Edits an existing monitoring point from the set of points in the command's state.
 * Does not persist the change (no monitoring point set yet exists)
 */
abstract class EditMonitoringPointCommand(val dept: Department, val pointIndex: Int)
	extends CommandInternal[Unit] with EditMonitoringPointState {

	override def applyInternal() = {
		copyTo(monitoringPoints.get(pointIndex))
	}
}

trait EditMonitoringPointValidation extends SelfValidating with MonitoringPointValidation {
	self: EditMonitoringPointState =>

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

		val pointsWithCurrentRemoved = monitoringPoints.asScala.zipWithIndex.filter(_._2 != pointIndex).unzip._1
		if (pointsWithCurrentRemoved.count(p =>
			p.name == name && p.validFromWeek == validFromWeek && p.requiredFromWeek == requiredFromWeek
		) > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("validFromWeek", "monitoringPoint.name.exists")
		}
	}
}

trait EditMonitoringPointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditMonitoringPointState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.MonitoringPoints.Manage, mandatory(dept))) ++
			dept.routes.asScala.map { route => CheckablePermission(Permissions.MonitoringPoints.Manage, route) }
		)
	}
}

trait EditMonitoringPointState extends MonitoringPointState {
	val pointIndex: Int
}

