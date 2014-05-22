package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.AttendanceMonitoringService

class AttendanceMonitoringSchemeIdConverter extends TwoWayConverter[String, AttendanceMonitoringScheme] {

	@Autowired var service: AttendanceMonitoringService = _

	override def convertRight(id: String) = (Option(id) flatMap { service.getSchemeById }).orNull
	override def convertLeft(scheme: AttendanceMonitoringScheme) = (Option(scheme) map {_.id}).orNull

}
