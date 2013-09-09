package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSetTemplate, MonitoringPoint}
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}
import org.springframework.validation.Errors
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import org.springframework.util.AutoPopulatingList
import uk.ac.warwick.tabula.helpers.StringUtils._

object AddMonitoringPointSetTemplateCommand {
	def apply() =
		new AddMonitoringPointSetTemplateCommand
		with ComposableCommand[MonitoringPointSetTemplate]
		with AutowiringMonitoringPointServiceComponent
		with MonitoringPointSetTemplatesPermissions
		with AddMonitoringPointSetTemplateDescription
		with AddMonitoringPointSetTemplateValidation
}


abstract class AddMonitoringPointSetTemplateCommand extends CommandInternal[MonitoringPointSetTemplate]
	with AddMonitoringPointSetTemplateState {
	self: MonitoringPointServiceComponent =>

	override def applyInternal() = {
		val set = new MonitoringPointSetTemplate
		set.templateName = templateName
		set.createdDate = new DateTime()
		set.points = monitoringPoints.asScala.map{m =>
			val point = new MonitoringPoint
			point.createdDate = new DateTime()
			point.defaultValue = m.defaultValue
			point.name = m.name
			point.pointSet = set
			point.updatedDate = new DateTime()
			point.week = m.week
			point
		}.asJava
		set.updatedDate = new DateTime()
		monitoringPointService.saveOrUpdate(set)
		set
	}
}

trait AddMonitoringPointSetTemplateValidation extends SelfValidating with MonitoringPointValidation {
	self: AddMonitoringPointSetTemplateState with MonitoringPointServiceComponent =>

	override def validate(errors: Errors) {
		if (monitoringPoints.size() == 0) {
			errors.rejectValue("monitoringPoints", "monitoringPointSet.noPointsTemplate")
		} else {
			monitoringPoints.asScala.zipWithIndex.foreach{case (point, index) => {
				validateName(errors, point.name, s"monitoringPoints[$index].name")
				validateWeek(errors, point.week, s"monitoringPoints[$index].week")

				if (monitoringPoints.asScala.count(p => p.name == point.name && p.week == point.week) > 1) {
					errors.rejectValue(s"monitoringPoints[$index].name", "monitoringPoint.name.exists")
				}
			}}
		}

		if (!templateName.hasText) {
			errors.rejectValue("templateName", "NotEmpty")
		} else if (templateName.length > 255) {
			errors.rejectValue("templateName", "monitoringPointSet.templateName.toolong")
		} else if (monitoringPointService.listTemplates.count(_.templateName.equals(templateName)) > 0) {
			errors.rejectValue("templateName", "monitoringPointSet.templateName.duplicate")
		}
	}
}

trait AddMonitoringPointSetTemplateDescription extends Describable[MonitoringPointSetTemplate] {
	self: AddMonitoringPointSetTemplateState =>

	override lazy val eventName = "AddMonitoringPointSetTemplate"

	override def describe(d: Description) {
		d.property("templateName", templateName)
	}
}

trait AddMonitoringPointSetTemplateState {

	var templateName: String = ""

	val monitoringPoints = new AutoPopulatingList(classOf[MonitoringPoint])
}
