package uk.ac.warwick.tabula.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointTemplate, MonitoringPointSetTemplate}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors

object RemoveMonitoringPointCommand {
	def apply(template: MonitoringPointSetTemplate, point: MonitoringPointTemplate) =
		new RemoveMonitoringPointCommand(template, point)
		with ComposableCommand[MonitoringPointTemplate]
		with RemoveMonitoringPointValidation
		with RemoveMonitoringPointDescription
		with MonitoringPointSetTemplatesPermissions
}

/**
 * Deletes an existing monitoring point.
 */
abstract class RemoveMonitoringPointCommand(val template: MonitoringPointSetTemplate, val point: MonitoringPointTemplate)
	extends CommandInternal[MonitoringPointTemplate] with RemoveMonitoringPointState {

	override def applyInternal() = {
		template.remove(point)
		point
	}
}

trait RemoveMonitoringPointValidation extends SelfValidating {
	self: RemoveMonitoringPointState =>

	override def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "monitoringPoint.delete.confirm")
	}
}

trait RemoveMonitoringPointDescription extends Describable[MonitoringPointTemplate] {
	self: RemoveMonitoringPointState =>

	override lazy val eventName = "RemoveMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSetTemplate(template)
		d.monitoringPointTemplate(point)
	}
}

trait RemoveMonitoringPointState {
	def template: MonitoringPointSetTemplate
	def point: MonitoringPointTemplate
	var confirm: Boolean = _
}

