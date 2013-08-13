package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters._
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.{AutowiringRouteDaoComponent, RouteDaoComponent}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet

trait RouteServiceComponent {
	def routeService: RouteService
}

trait AutowiringRouteServiceComponent extends RouteServiceComponent {
	var routeService = Wire[RouteService]
}

trait RouteService {
	def save(route: Route)
	def save(set: MonitoringPointSet)
	def getByCode(code: String): Option[Route]
	def findMonitoringPointSet(route: Route, year: Option[Int]): Option[MonitoringPointSet]
}

abstract class AbstractRouteService extends RouteService {
	self: RouteDaoComponent =>

	def save(route: Route) = routeDao.saveOrUpdate(route)
	def save(set: MonitoringPointSet) = routeDao.save(set)
	def getByCode(code: String): Option[Route] = routeDao.getByCode(code)
	def findMonitoringPointSet(route: Route, year: Option[Int]) = routeDao.findMonitoringPointSet(route, year)
}

@Service("routeService")
class RouteServiceImpl 
	extends AbstractRouteService
		with AutowiringRouteDaoComponent