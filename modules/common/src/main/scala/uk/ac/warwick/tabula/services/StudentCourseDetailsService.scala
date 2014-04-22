package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{StudentCourseDetailsDaoComponent, AutowiringStudentCourseDetailsDaoComponent}

/**
 * Handles data about studentCourseDetailss
 */
trait StudentCourseDetailsServiceComponent {
	def studentCourseDetailsService: StudentCourseDetailsService
}

trait AutowiringStudentCourseDetailsServiceComponent extends StudentCourseDetailsServiceComponent {
	var studentCourseDetailsService = Wire[StudentCourseDetailsService]
}

trait StudentCourseDetailsService {
	def studentCourseDetailsFromScjCode(code: String): Option[StudentCourseDetails]
}

abstract class AbstractStudentCourseDetailsService extends StudentCourseDetailsService {
	self: StudentCourseDetailsDaoComponent =>

	def studentCourseDetailsFromScjCode(scjCode: String): Option[StudentCourseDetails] = scjCode.maybeText.flatMap {
		someCode => studentCourseDetailsDao.getByScjCode(someCode.toLowerCase)
	}
}

@Service ("studentCourseDetailsService")
class StudentCourseDetailsServiceImpl extends AbstractStudentCourseDetailsService with AutowiringStudentCourseDetailsDaoComponent
