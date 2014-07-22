package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AutowiringCourseDaoComponent, AutowiringRouteDaoComponent, CourseDaoComponent, RouteDaoComponent}
import uk.ac.warwick.tabula.data.model.{Course, Route}
import uk.ac.warwick.tabula.helpers.StringUtils._

/**
 * Handles data about courses and routes
 */
trait CourseAndRouteServiceComponent {
	def courseAndRouteService: CourseAndRouteService
}

trait AutowiringCourseAndRouteServiceComponent extends CourseAndRouteServiceComponent {
	var courseAndRouteService = Wire[CourseAndRouteService]
}

trait CourseAndRouteService extends RouteDaoComponent with CourseDaoComponent {
	def save(route: Route) = routeDao.saveOrUpdate(route)
	def getRouteById(id: String): Option[Route] = routeDao.getById(id)

	def getRouteByCode(code: String): Option[Route] = code.maybeText.flatMap {
		rcode => routeDao.getByCode(rcode.toLowerCase)
	}

	def getCourseByCode(code: String): Option[Course] = code.maybeText.flatMap {
		ccode => courseDao.getByCode(ccode.toLowerCase)
	}

	def findRoutesNamedLike(query: String): Seq[Route] =
		routeDao.findRoutesNamedLike(query)
}

abstract class AbstractCourseAndRouteService extends CourseAndRouteService {
	self: RouteDaoComponent with CourseDaoComponent =>

}

@Service("courseAndRouteService")
class CourseAndRouteServiceImpl 
	extends AbstractCourseAndRouteService
		with AutowiringRouteDaoComponent
		with AutowiringCourseDaoComponent
