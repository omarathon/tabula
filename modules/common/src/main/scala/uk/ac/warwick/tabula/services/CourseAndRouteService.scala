package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.AutowiringCourseDaoComponent
import uk.ac.warwick.tabula.data.AutowiringRouteDaoComponent
import uk.ac.warwick.tabula.data.CourseDao
import uk.ac.warwick.tabula.data.CourseDaoComponent
import uk.ac.warwick.tabula.data.ModeOfAttendanceDao
import uk.ac.warwick.tabula.data.RouteDao
import uk.ac.warwick.tabula.data.RouteDaoComponent
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.tabula.data.model.Route

/**
 * Handles data about courses and routes
 */
trait CourseAndRouteServiceComponent {
	def courseAndRouteService: CourseAndRouteService
}

trait AutowiringCourseAndRouteServiceComponent extends CourseAndRouteServiceComponent {
	var courseAndRouteService = Wire[CourseAndRouteService]
}

trait CourseAndRouteService {
	def save(route: Route)
	def getRouteByCode(code: String): Option[Route]
	def getRouteById(id: String): Option[Route]
	def getCourseByCode(code: String): Option[Course]
}

abstract class AbstractCourseAndRouteService extends CourseAndRouteService {
	self: RouteDaoComponent with CourseDaoComponent =>

	def save(route: Route) = routeDao.saveOrUpdate(route)
	def getRouteByCode(code: String): Option[Route] = routeDao.getByCode(code)
	def getRouteById(id: String): Option[Route] = routeDao.getById(id)
	def getCourseByCode(code: String): Option[Course] = courseDao.getByCode(code)
}

@Service("courseAndRouteService")
class CourseAndRouteServiceImpl 
	extends AbstractCourseAndRouteService
		with AutowiringRouteDaoComponent
		with AutowiringCourseDaoComponent
