package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.MonitoringPointService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.attendance.{AbstractMonitoringPointSet, MonitoringPointSet}

class AbstractMonitoringPointSetIdConverter extends TwoWayConverter[String, AbstractMonitoringPointSet] {

	@Autowired var service: MonitoringPointService = _

	override def convertRight(id: String) = (Option(id) flatMap { id =>
		service.getSetById(id) match {
			case Some(set: MonitoringPointSet) => Option(set)
			case None => service.getTemplateById(id)
		}
	}).orNull
	override def convertLeft(set: AbstractMonitoringPointSet) = (Option(set) map {_.id}).orNull

}
