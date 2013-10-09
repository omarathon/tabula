package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.{AutowiringRouteDaoComponent, RouteDaoComponent}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Route

trait RouteServiceComponent {
	def routeService: RouteService
}

trait AutowiringRouteServiceComponent extends RouteServiceComponent {
	var routeService = Wire[RouteService]
}

trait RouteService {
	def save(route: Route)
	def getByCode(code: String): Option[Route]
	def getById(id: String): Option[Route]
}

abstract class AbstractRouteService extends RouteService {
	self: RouteDaoComponent =>

	def save(route: Route) = routeDao.saveOrUpdate(route)
	def getByCode(code: String): Option[Route] = routeDao.getByCode(code)
	def getById(id: String): Option[Route] = routeDao.getById(id)
}

@Service("routeService")
class RouteServiceImpl 
	extends AbstractRouteService
		with AutowiringRouteDaoComponent