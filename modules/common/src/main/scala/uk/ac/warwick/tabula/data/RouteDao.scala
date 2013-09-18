package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository

import uk.ac.warwick.tabula.data.model.{Department, Route}
import uk.ac.warwick.spring.Wire

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
}

@Repository
class RouteDaoImpl extends RouteDao with Daoisms {

	def saveOrUpdate(route: Route) = session.saveOrUpdate(route)

	def getByCode(code: String) =
		session.newQuery[Route]("from Route r where code = :code").setString("code", code).uniqueResult



	def findByDepartment(department:Department) =
		session.newQuery[Route]("from Route r where department = :dept").setEntity("dept",department).seq

}