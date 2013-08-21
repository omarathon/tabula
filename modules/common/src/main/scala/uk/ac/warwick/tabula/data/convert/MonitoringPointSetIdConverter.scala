package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.RouteService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet

class MonitoringPointSetIdConverter extends TwoWayConverter[String, MonitoringPointSet] {

	@Autowired var service: RouteService = _

	override def convertRight(id: String) = (Option(id) flatMap { service.getMonitoringPointSetById(_) }).orNull
	override def convertLeft(set: MonitoringPointSet) = (Option(set) map {_.id}).orNull

}
