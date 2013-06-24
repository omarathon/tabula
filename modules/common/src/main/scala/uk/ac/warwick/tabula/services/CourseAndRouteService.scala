package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.RouteDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.CourseDao

/**
 * Handles data about courses and routes
 */
@Service
class CourseAndRouteService extends Logging {

	@Autowired var routeDao: RouteDao = _
	@Autowired var courseDao: CourseDao = _

	def getRouteByCode(code: String) = transactional(readOnly = true) {
		routeDao.getByCode(code)
	}

	def getCourseByCode(code: String) = transactional(readOnly = true) {
		val ret = courseDao.getByCode(code)
		ret
	}
}
