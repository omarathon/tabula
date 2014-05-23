package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.services.AttendanceMonitoringService
import org.springframework.beans.factory.annotation.Autowired

class AttendanceMonitoringPointIdConverter extends TwoWayConverter[String, AttendanceMonitoringPoint] {

	@Autowired var service: AttendanceMonitoringService = _

	override def convertRight(id: String) = (Option(id) flatMap { service.getPointById }).orNull
	override def convertLeft(point: AttendanceMonitoringPoint) = (Option(point) map {_.id}).orNull

}
