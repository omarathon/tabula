package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import model.Module
import org.hibernate.`type`._

import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import model.Department
import uk.ac.warwick.tabula.data.model.Route

trait RouteDao {
	def saveOrUpdate(route: Route)
	def getByCode(code: String): Option[Route]
}

@Repository
class RouteDaoImpl extends RouteDao with Daoisms {

	def saveOrUpdate(route: Route) = session.saveOrUpdate(route)

	def getByCode(code: String) = 
		session.newQuery[Route]("from Route r where code = :code").setString("code", code).uniqueResult

}