package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import scala.collection.JavaConverters._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions


object UpdateMonitoringPointCommand {
	def apply(set: MonitoringPointSet, point: MonitoringPoint) =
		new UpdateMonitoringPointCommand(set, point)
		with ComposableCommand[MonitoringPoint]
		with AutowiringMonitoringPointServiceComponent
		with AutowiringTermServiceComponent
		with UpdateMonitoringPointValidation
		with UpdateMonitoringPointDescription
		with UpdateMonitoringPointPermission
}

/**
 * Update a monitoring point
 */
abstract class UpdateMonitoringPointCommand(val set: MonitoringPointSet, val point: MonitoringPoint)
	extends CommandInternal[MonitoringPoint] with UpdateMonitoringPointState {

	self: MonitoringPointServiceComponent =>

	copyFrom(point)

	override def applyInternal() = {
		copyTo(point)
		point.updatedDate = new DateTime()
		monitoringPointService.saveOrUpdate(point)
		point
	}
}

trait UpdateMonitoringPointValidation extends SelfValidating with MonitoringPointValidation {
	self: UpdateMonitoringPointState with MonitoringPointServiceComponent =>

	override def validate(errors: Errors) {
		if (point.sentToAcademicOffice) {
			errors.reject("monitoringPoint.sentToAcademicOffice.points.update")
		} else if (monitoringPointService.countCheckpointsForPoint(point) > 0) {
			errors.reject("monitoringPoint.hasCheckpoints.update")
		}

		validateWeek(errors, validFromWeek, "validFromWeek")
		validateWeek(errors, requiredFromWeek, "requiredFromWeek")
		validateWeeks(errors, validFromWeek, requiredFromWeek, "validFromWeek")
		validateName(errors, name, "name")

		if (set.points.asScala.count(p =>
			p.name == name && p.validFromWeek == validFromWeek && p.requiredFromWeek == requiredFromWeek && p.id != point.id
		) > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("validFromWeek", "monitoringPoint.name.exists")
		}
	}
}

trait UpdateMonitoringPointPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: UpdateMonitoringPointState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, mandatory(set.route))
	}
}

trait UpdateMonitoringPointDescription extends Describable[MonitoringPoint] {
	self: UpdateMonitoringPointState =>

	override lazy val eventName = "UpdateMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSet(set)
		d.monitoringPoint(point)
	}
}

trait UpdateMonitoringPointState extends GroupMonitoringPointsByTerm with CanPointBeChanged {
	def set: MonitoringPointSet
	def point: MonitoringPoint
	val academicYear = set.academicYear
	val dept = set.route.department
	var name: String = _
	var validFromWeek: Int = 0
	var requiredFromWeek: Int = 0
	def monitoringPointsByTerm = groupByTerm(set.points.asScala, academicYear)

	def copyTo(point: MonitoringPoint) {
		point.name = this.name
		point.validFromWeek = this.validFromWeek
		point.requiredFromWeek = this.requiredFromWeek
	}

	def copyFrom(point: MonitoringPoint) {
		this.name = point.name
		this.validFromWeek = point.validFromWeek
		this.requiredFromWeek = point.requiredFromWeek
	}
}