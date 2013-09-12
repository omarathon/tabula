package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSetTemplate, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors

object RemoveMonitoringPointCommand {
	def apply(template: MonitoringPointSetTemplate, point: MonitoringPoint) =
		new RemoveMonitoringPointCommand(template, point)
		with ComposableCommand[MonitoringPoint]
		with RemoveMonitoringPointValidation
		with RemoveMonitoringPointDescription
		with MonitoringPointSetTemplatesPermissions
}

/**
 * Deletes an existing monitoring point.
 */
abstract class RemoveMonitoringPointCommand(val template: MonitoringPointSetTemplate, val point: MonitoringPoint)
	extends CommandInternal[MonitoringPoint] with RemoveMonitoringPointState {

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

trait RemoveMonitoringPointDescription extends Describable[MonitoringPoint] {
	self: RemoveMonitoringPointState =>

	override lazy val eventName = "RemoveMonitoringPoint"

	override def describe(d: Description) {
		d.monitoringPointSetTemplate(template)
		d.monitoringPoint(point)
	}
}

trait RemoveMonitoringPointState {
	def template: MonitoringPointSetTemplate
	def point: MonitoringPoint
	var confirm: Boolean = _
}

