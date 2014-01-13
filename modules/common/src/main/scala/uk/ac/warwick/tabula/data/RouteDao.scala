package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.{Department, Route}
import uk.ac.warwick.spring.Wire
import org.hibernate.criterion.Order

trait RouteDaoComponent {
	val routeDao: RouteDao
}

trait AutowiringRouteDaoComponent extends RouteDaoComponent {
	val routeDao = Wire[RouteDao]
}

trait RouteDao {
	def allRoutes: Seq[Route]
	def saveOrUpdate(route: Route)
	def getByCode(code: String): Option[Route]
	def getById(id: String): Option[Route]
	def findByDepartment(department:Department):Seq[Route]
}

@Repository
class RouteDaoImpl extends RouteDao with Daoisms {

	def allRoutes: Seq[Route] =
		session.newCriteria[Route]
			.addOrder(Order.asc("code"))
			.seq
			.distinct

	def saveOrUpdate(route: Route) = session.saveOrUpdate(route)

	def getByCode(code: String) =
		session.newQuery[Route]("from Route r where code = :code").setString("code", code).uniqueResult

	def getById(id: String) = getById[Route](id)

	def findByDepartment(department:Department) =
		session.newQuery[Route]("from Route r where department = :dept").setEntity("dept",department).seq

}