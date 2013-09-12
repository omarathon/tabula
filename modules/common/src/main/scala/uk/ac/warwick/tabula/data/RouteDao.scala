package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import org.hibernate.criterion.Restrictions._

import uk.ac.warwick.tabula.data.model.{Department, Route}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet

trait RouteDaoComponent {
	val routeDao: RouteDao
}

trait AutowiringRouteDaoComponent extends RouteDaoComponent {
	val routeDao = Wire[RouteDao]
}

trait RouteDao {
	def saveOrUpdate(route: Route)
	def getByCode(code: String): Option[Route]
	def findByDepartment(department:Department):Seq[Route]
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet]
	def findMonitoringPointSet(route: Route, year: Option[Int]): Option[MonitoringPointSet]
}

@Repository
class RouteDaoImpl extends RouteDao with Daoisms {

	def saveOrUpdate(route: Route) = session.saveOrUpdate(route)

	def getByCode(code: String) =
		session.newQuery[Route]("from Route r where code = :code").setString("code", code).uniqueResult

	def findMonitoringPointSets(route: Route) =
		session.newCriteria[MonitoringPointSet]
			.add(is("route", route))
			.seq

	def findMonitoringPointSet(route: Route, year: Option[Int]) =
		session.newCriteria[MonitoringPointSet]
			.add(is("route", route))
			.add(yearRestriction(year))
			.uniqueResult

	def findByDepartment(department:Department) =
		session.newQuery[Route]("from Route r where department = :dept").setEntity("dept",department).seq
	
	private def yearRestriction(opt: Option[Any]) = opt map { is("year", _) } getOrElse { isNull("year") }

}