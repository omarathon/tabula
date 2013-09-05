package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSetTemplate, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import scala.collection.JavaConverters._
import org.joda.time.DateTime


object CreateMonitoringPointCommand {
	def apply(template: MonitoringPointSetTemplate) =
		new CreateMonitoringPointCommand(template)
		with ComposableCommand[MonitoringPoint]
		with CreateMonitoringPointValidation
		with CreateMonitoringPointDescription
		with MonitoringPointSetTemplatesPermissions
}

/**
 * Create a new monitoring point for the given template.
 */
abstract class CreateMonitoringPointCommand(val template: MonitoringPointSetTemplate) extends CommandInternal[MonitoringPoint] with CreateMonitoringPointState {

	override def applyInternal() = {
		val point = new MonitoringPoint
		point.name = name
		point.defaultValue = defaultValue
		point.week = week
		point.createdDate = new DateTime()
		point.updatedDate = new DateTime()
		template.add(point)
		point
	}
}

trait CreateMonitoringPointValidation extends SelfValidating with MonitoringPointValidation {
	self: CreateMonitoringPointState =>

	override def validate(errors: Errors) {
		validateWeek(errors, week, "week")
		validateName(errors, name, "name")

		if (template.points.asScala.count(p => p.name == name && p.week == week) > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("week", "monitoringPoint.name.exists")
		}
	}
}

trait CreateMonitoringPointDescription extends Describable[MonitoringPoint] {
	self: CreateMonitoringPointState =>

	override lazy val eventName = "CreateMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSetTemplate(template)
		d.property("name", name)
		d.property("week", week)
		d.property("defaultValue", defaultValue)
	}
}

trait CreateMonitoringPointState {
	def template: MonitoringPointSetTemplate
	var name: String = _
	var defaultValue: Boolean = true
	var week: Int = 0
}

