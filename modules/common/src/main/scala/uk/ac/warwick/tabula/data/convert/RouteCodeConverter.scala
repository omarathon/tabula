package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.RouteService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.Route

class RouteCodeConverter extends TwoWayConverter[String, Route] {

	@Autowired var service: RouteService = _

	override def convertRight(id: String) = (Option(id) flatMap { service.getByCode(_) }).orNull
	override def convertLeft(route: Route) = (Option(route) map {_.id}).orNull

}
