package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSetTemplate
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}
import org.springframework.validation.Errors
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.StringUtils._

object EditMonitoringPointSetTemplateCommand {
	def apply(template: MonitoringPointSetTemplate) =
		new EditMonitoringPointSetTemplateCommand(template)
		with ComposableCommand[MonitoringPointSetTemplate]
		with AutowiringMonitoringPointServiceComponent
		with MonitoringPointSetTemplatesPermissions
		with EditMonitoringPointSetTemplateDescription
		with EditMonitoringPointSetTemplateValidation
}


abstract class EditMonitoringPointSetTemplateCommand(val template: MonitoringPointSetTemplate) extends CommandInternal[MonitoringPointSetTemplate]
	with EditMonitoringPointSetTemplateState {
	self: MonitoringPointServiceComponent =>

	templateName = template.templateName

	override def applyInternal() = {
		template.templateName = templateName
		template.updatedDate = new DateTime()
		template
	}
}

trait EditMonitoringPointSetTemplateValidation extends SelfValidating {
	self: EditMonitoringPointSetTemplateState with MonitoringPointServiceComponent =>

	override def validate(errors: Errors) {
		if (!templateName.hasText) {
			errors.rejectValue("templateName", "NotEmpty")
		} else if (templateName.length > 255) {
			errors.rejectValue("templateName", "monitoringPointSet.templateName.toolong")
		} else if (monitoringPointService.listTemplates.count(t => t.templateName.equals(templateName) && !t.getId.equals(template.getId)) > 0) {
			errors.rejectValue("templateName", "monitoringPointSet.templateName.duplicate")
		}
	}
}

trait EditMonitoringPointSetTemplateDescription extends Describable[MonitoringPointSetTemplate] {
	self: EditMonitoringPointSetTemplateState =>

	override lazy val eventName = "EditMonitoringPointSetTemplate"

	override def describe(d: Description) {
		d.monitoringPointSetTemplate(template)
	}
}

trait EditMonitoringPointSetTemplateState {

	def template: MonitoringPointSetTemplate

	var templateName: String = ""
}
