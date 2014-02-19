package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.MonitoringPointService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointTemplate

class MonitoringPointTemplateIdConverter extends TwoWayConverter[String, MonitoringPointTemplate] {

	@Autowired var service: MonitoringPointService = _

	override def convertRight(id: String) = (Option(id) flatMap { service.getPointTemplateById(_) }).orNull
	override def convertLeft(set: MonitoringPointTemplate) = (Option(set) map {_.id}).orNull

}
