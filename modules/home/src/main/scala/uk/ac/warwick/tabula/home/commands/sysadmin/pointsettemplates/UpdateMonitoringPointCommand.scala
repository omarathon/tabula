package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSetTemplate, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import scala.collection.JavaConverters._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}


object UpdateMonitoringPointCommand {
	def apply(template: MonitoringPointSetTemplate, point: MonitoringPoint) =
		new UpdateMonitoringPointCommand(template, point)
		with ComposableCommand[MonitoringPoint]
		with AutowiringMonitoringPointServiceComponent
		with UpdateMonitoringPointValidation
		with UpdateMonitoringPointDescription
		with MonitoringPointSetTemplatesPermissions
}

/**
 * Update a monitoring point
 */
abstract class UpdateMonitoringPointCommand(val template: MonitoringPointSetTemplate, val point: MonitoringPoint)
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
	self: UpdateMonitoringPointState =>

	override def validate(errors: Errors) {
		validateWeek(errors, week, "week")
		validateName(errors, name, "name")

		if (template.points.asScala.count(p => p.name == name && p.week == week && p.id != point.id) > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("week", "monitoringPoint.name.exists")
		}
	}
}

trait UpdateMonitoringPointDescription extends Describable[MonitoringPoint] {
	self: UpdateMonitoringPointState =>

	override lazy val eventName = "UpdateMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSetTemplate(template)
		d.monitoringPoint(point)
	}
}

trait UpdateMonitoringPointState {
	def template: MonitoringPointSetTemplate
	def point: MonitoringPoint
	var name: String = _
	var defaultValue: Boolean = true
	var week: Int = 0

	def copyTo(point: MonitoringPoint) {
		point.name = this.name
		point.defaultValue = this.defaultValue
		point.week = this.week
	}

	def copyFrom(point: MonitoringPoint) {
		this.name = point.name
		this.defaultValue = point.defaultValue
		this.week = point.week
	}
}

