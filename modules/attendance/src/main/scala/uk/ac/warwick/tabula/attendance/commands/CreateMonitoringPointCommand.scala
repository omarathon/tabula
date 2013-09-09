package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import scala.collection.JavaConverters._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, AutowiringTermServiceComponent}


object CreateMonitoringPointCommand {
	def apply(set: MonitoringPointSet) =
		new CreateMonitoringPointCommand(set)
		with ComposableCommand[MonitoringPoint]
		with CreateMonitoringPointValidation
		with CreateMonitoringPointDescription
		with CreateMonitoringPointPermission
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
}

/**
 * Create a new monitoring point for the given set.
 */
abstract class CreateMonitoringPointCommand(val set: MonitoringPointSet) extends CommandInternal[MonitoringPoint] with CreateMonitoringPointState {

	override def applyInternal() = {
		val point = new MonitoringPoint
		point.name = name
		point.defaultValue = defaultValue
		point.week = week
		point.createdDate = new DateTime()
		point.updatedDate = new DateTime()
		set.add(point)
		point
	}
}

trait CreateMonitoringPointValidation extends SelfValidating with MonitoringPointValidation {
	self: CreateMonitoringPointState =>

	override def validate(errors: Errors) {
		if (set.sentToAcademicOffice) {
			errors.reject("monitoringPointSet.sentToAcademicOffice.points.create")
		}

		validateWeek(errors, week, "week")
		validateName(errors, name, "name")

		if (set.points.asScala.count(p => p.name == name && p.week == week) > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("week", "monitoringPoint.name.exists")
		}
	}
}

trait CreateMonitoringPointPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateMonitoringPointState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, mandatory(set.route))
	}
}

trait CreateMonitoringPointDescription extends Describable[MonitoringPoint] {
	self: CreateMonitoringPointState =>

	override lazy val eventName = "CreateMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSet(set)
		d.property("name", name)
		d.property("week", week)
		d.property("defaultValue", defaultValue)
	}
}

trait CreateMonitoringPointState extends GroupMonitoringPointsByTerm with CanPointBeChanged {
	def set: MonitoringPointSet
	val academicYear = set.academicYear
	val dept = set.route.department
	var name: String = _
	var defaultValue: Boolean = true
	var week: Int = 0

	def monitoringPointsByTerm = groupByTerm(set.points.asScala, set.academicYear)
}

