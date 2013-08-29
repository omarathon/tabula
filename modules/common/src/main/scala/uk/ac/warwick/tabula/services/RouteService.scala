package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.{AutowiringRouteDaoComponent, RouteDaoComponent}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}

trait RouteServiceComponent {
	def routeService: RouteService
}

trait AutowiringRouteServiceComponent extends RouteServiceComponent {
	var routeService = Wire[RouteService]
}

trait RouteService {
	def save(route: Route)
	def save(set: MonitoringPointSet)
	def save(point: MonitoringPoint)
	def delete(point: MonitoringPoint)
	def getByCode(code: String): Option[Route]
	def getMonitoringPointSetById(id: String): Option[MonitoringPointSet]
	def getMonitoringPointById(id: String): Option[MonitoringPoint]
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet]
	def findMonitoringPointSet(route: Route, year: Option[Int]): Option[MonitoringPointSet]
}

abstract class AbstractRouteService extends RouteService {
	self: RouteDaoComponent =>

	def save(route: Route) = routeDao.saveOrUpdate(route)
	def save(set: MonitoringPointSet) = routeDao.save(set)
	def save(point: MonitoringPoint) = routeDao.save(point)
	def delete(point: MonitoringPoint) = routeDao.delete(point)
	def getByCode(code: String): Option[Route] = routeDao.getByCode(code)
	def getMonitoringPointSetById(id: String): Option[MonitoringPointSet] = routeDao.getMonitoringPointSetById(id)
	def getMonitoringPointById(id: String): Option[MonitoringPoint] = routeDao.getMonitoringPointById(id)
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet] = routeDao.findMonitoringPointSets(route)
	def findMonitoringPointSet(route: Route, year: Option[Int]) = routeDao.findMonitoringPointSet(route, year)
}

@Service("routeService")
class RouteServiceImpl 
	extends AbstractRouteService
		with AutowiringRouteDaoComponent