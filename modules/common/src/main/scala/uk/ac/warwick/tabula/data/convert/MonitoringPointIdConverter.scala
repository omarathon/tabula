package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.MonitoringPointService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint

class MonitoringPointIdConverter extends TwoWayConverter[String, MonitoringPoint] {

	@Autowired var service: MonitoringPointService = _

	override def convertRight(id: String) = (Option(id) flatMap { service.getPointById(_) }).orNull
	override def convertLeft(set: MonitoringPoint) = (Option(set) map {_.id}).orNull

}
