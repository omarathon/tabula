package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{AutowiringRouteServiceComponent, RouteServiceComponent}

object ModifyMonitoringPointCommand {
	def apply(set: MonitoringPointSet, point: MonitoringPoint) =
		new ModifyMonitoringPointCommand(set, point)
			with ComposableCommand[MonitoringPoint]
			with ModifyMonitoringPointValidation
			with ModifyMonitoringPointPermissions
			with ModifyMonitoringPointState
			with ModifyMonitoringPointDescription
			with AutowiringRouteServiceComponent
}

/**
 * Creates a new monitoring point in a set.
 */
class ModifyMonitoringPointCommand(val set: MonitoringPointSet, val thePoint: MonitoringPoint) extends CommandInternal[MonitoringPoint] {
	self: ModifyMonitoringPointState with RouteServiceComponent =>

	point = thePoint
	copyFrom(point)

	override def applyInternal() = {
		this.copyTo(point)
		routeService.save(point)
		point
	}
}

trait ModifyMonitoringPointPermissions extends RequiresPermissionsChecking {
	self: ModifyMonitoringPointState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, set.route)
	}
}

trait MonitoringPointValidation extends SelfValidating {
	self: ModifyMonitoringPointState =>

	override def validate(errors: Errors) {
		week match {
			case y if y < 1  => errors.rejectValue("week", "monitoringPointSet.week.min")
			case y if y > 52 => errors.rejectValue("week", "monitoringPointSet.week.max")
			case _ =>
		}

		if (!name.hasText) {
			errors.rejectValue("name", "NotEmpty")
		} else if (name.length > 4000) {
			errors.rejectValue("name", "monitoringPoint.name.toolong")
		}
	}
}

trait ModifyMonitoringPointValidation extends MonitoringPointValidation {
	self: ModifyMonitoringPointState =>

	override def validate(errors: Errors) {
		super.validate(errors)

		if (set.points.asScala.filter(p => p.name == name && p.week == week && p.id != point.id).size > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("week", "monitoringPoint.name.exists")
		}
	}
}

trait ModifyMonitoringPointDescription extends Describable[MonitoringPoint] {
	self: ModifyMonitoringPointState =>

	override lazy val eventName = "ModifyMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSet(set)
		d.property("name", name)
	}
}

trait ModifyMonitoringPointState {
	val set: MonitoringPointSet

	var name: String = _
	var defaultValue: Boolean = _
	var week: Int = _
	var point: MonitoringPoint = _

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