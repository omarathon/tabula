package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import model.Module
import org.hibernate.`type`._
import org.springframework.beans.factory.annotation.Autowired
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import model.Department
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import org.hibernate.criterion.Restrictions

trait RouteDaoComponent {
	val routeDao: RouteDao
}

trait AutowiringRouteDaoComponent extends RouteDaoComponent {
	val routeDao = Wire[RouteDao]
}

trait RouteDao {
	def saveOrUpdate(route: Route)
	def getByCode(code: String): Option[Route]
	def findMonitoringPointSet(route: Route, year: Option[Int]): Option[MonitoringPointSet]
}

@Repository
class RouteDaoImpl extends RouteDao with Daoisms {

	def saveOrUpdate(route: Route) = session.saveOrUpdate(route)

	def getByCode(code: String) =
		session.newQuery[Route]("from Route r where code = :code").setString("code", code).uniqueResult

	def findMonitoringPointSet(route: Route, year: Option[Int]) = {
		year match {
			case Some(year) => session.newCriteria[MonitoringPointSet]
													.add(Restrictions.eq("route", route))
													.add(Restrictions.eq("year", year))
													.uniqueResult
			case _ => session.newCriteria[MonitoringPointSet]
									.add(Restrictions.eq("route", route))
									.add(Restrictions.isNull("year"))
									.uniqueResult
		}
		
	}
}