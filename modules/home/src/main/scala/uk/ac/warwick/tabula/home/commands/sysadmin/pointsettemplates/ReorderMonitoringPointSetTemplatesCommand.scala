package uk.ac.warwick.tabula.home.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.services.{MonitoringPointServiceComponent, AutowiringMonitoringPointServiceComponent}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSetTemplate
import scala.collection.JavaConverters._
import org.springframework.util.AutoPopulatingList

object ReorderMonitoringPointSetTemplatesCommand {
	def apply() =
		new ReorderMonitoringPointSetTemplatesCommandInternal
			with ComposableCommand[Seq[MonitoringPointSetTemplate]]
			with AutowiringMonitoringPointServiceComponent
			with MonitoringPointSetTemplatesPermissions
			with ReorderMonitoringPointSetTemplatesDescription
			with ReorderMonitoringPointSetTemplatesState
}

class ReorderMonitoringPointSetTemplatesCommandInternal
	extends CommandInternal[Seq[MonitoringPointSetTemplate]] with ReorderMonitoringPointSetTemplatesState {

	this: MonitoringPointServiceComponent =>

	override def applyInternal() = {
		templates.asScala.zipWithIndex.map{case (template, index) =>
			template.position = index
			monitoringPointService.saveOrUpdate(template)
			template
		}.toSeq
	}
}

trait ReorderMonitoringPointSetTemplatesDescription extends Describable[Seq[MonitoringPointSetTemplate]] {
	self: ReorderMonitoringPointSetTemplatesState =>

	override lazy val eventName = "ReorderMonitoringPointSetTemplates"

	override def describe(d: Description) {
		d.property("templates", templates.asScala.zipWithIndex.map{case (template, index) => index -> template.templateName })
	}
}

trait ReorderMonitoringPointSetTemplatesState {

	var templates = new AutoPopulatingList(classOf[MonitoringPointSetTemplate])
}