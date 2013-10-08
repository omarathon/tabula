package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.RouteService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.Route

class RouteCodeConverter extends TwoWayConverter[String, Route] {

	@Autowired var service: RouteService = _

	override def convertRight(code: String) = 
		service.getByCode(sanitise(code)).getOrElse {
			service.getById(code).orNull
		}
	
	override def convertLeft(route: Route) = (Option(route) map { _.code }).orNull

	def sanitise(code: String) = {
		if (code == null) throw new IllegalArgumentException
		else code.toLowerCase
	}

}
