package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.MonitoringPointService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSetTemplate

class MonitoringPointSetTemplateIdConverter extends TwoWayConverter[String, MonitoringPointSetTemplate] {

	@Autowired var service: MonitoringPointService = _

	override def convertRight(id: String) = (Option(id) flatMap { service.getTemplateById(_) }).orNull
	override def convertLeft(template: MonitoringPointSetTemplate) = (Option(template) map {_.id}).orNull

}
