package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSetTemplate
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}
import org.springframework.validation.Errors

object DeleteMonitoringPointSetTemplateCommand {
	def apply(template: MonitoringPointSetTemplate) =
		new DeleteMonitoringPointSetTemplateCommand(template)
		with ComposableCommand[MonitoringPointSetTemplate]
		with AutowiringMonitoringPointServiceComponent
		with MonitoringPointSetTemplatesPermissions
		with DeleteMonitoringPointSetTemplateDescription
		with DeleteMonitoringPointSetTemplateValidation
}


abstract class DeleteMonitoringPointSetTemplateCommand(val template: MonitoringPointSetTemplate) extends CommandInternal[MonitoringPointSetTemplate]
	with DeleteMonitoringPointSetTemplateState {
	self: MonitoringPointServiceComponent =>

	override def applyInternal() = {
		monitoringPointService.deleteTemplate(template)
		template
	}
}

trait DeleteMonitoringPointSetTemplateValidation extends SelfValidating {
	self: DeleteMonitoringPointSetTemplateState with MonitoringPointServiceComponent =>

	override def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "monitoringPointSet.delete.confirm")
	}
}

trait DeleteMonitoringPointSetTemplateDescription extends Describable[MonitoringPointSetTemplate] {
	self: DeleteMonitoringPointSetTemplateState =>

	override lazy val eventName = "DeleteMonitoringPointSetTemplate"

	override def describe(d: Description) {
		d.monitoringPointSetTemplate(template)
	}
}

trait DeleteMonitoringPointSetTemplateState {

	def template: MonitoringPointSetTemplate

	var confirm: Boolean = false
}
