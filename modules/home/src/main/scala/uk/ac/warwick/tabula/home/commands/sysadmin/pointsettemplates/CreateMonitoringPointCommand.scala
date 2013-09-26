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
		point.validFromWeek = validFromWeek
		point.requiredFromWeek = requiredFromWeek
		point.createdDate = new DateTime()
		point.updatedDate = new DateTime()
		template.add(point)
		point
	}
}

trait CreateMonitoringPointValidation extends SelfValidating with MonitoringPointValidation {
	self: CreateMonitoringPointState =>

	override def validate(errors: Errors) {
		validateWeek(errors, validFromWeek, "validFromWeek")
		validateWeek(errors, requiredFromWeek, "requiredFromWeek")
		validateWeeks(errors, validFromWeek, requiredFromWeek, "validFromWeek")
		validateName(errors, name, "name")

		if (template.points.asScala.count(p =>
			p.name == name && p.validFromWeek == validFromWeek && p.requiredFromWeek == requiredFromWeek
		) > 0) {
			errors.rejectValue("name", "monitoringPoint.name.exists")
			errors.rejectValue("validFromWeek", "monitoringPoint.name.exists")
		}
	}
}

trait CreateMonitoringPointDescription extends Describable[MonitoringPoint] {
	self: CreateMonitoringPointState =>

	override lazy val eventName = "CreateMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSetTemplate(template)
		d.property("name", name)
		d.property("validFromWeek", validFromWeek)
		d.property("requiredFromWeek", requiredFromWeek)
	}
}

trait CreateMonitoringPointState {
	def template: MonitoringPointSetTemplate
	var name: String = _
	var validFromWeek: Int = 0
	var requiredFromWeek: Int = 0
}

