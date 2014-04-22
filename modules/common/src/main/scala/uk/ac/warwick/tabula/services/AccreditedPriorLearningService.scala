package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{Award, StudentCourseDetails, AccreditedPriorLearning}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AccreditedPriorLearningDaoComponent, AutowiringAccreditedPriorLearningDaoComponent}

/**
 * Handles data about accreditedPriorLearnings
 */
trait AccreditedPriorLearningServiceComponent {
	def accreditedPriorLearningService: AccreditedPriorLearningService
}

trait AutowiringAccreditedPriorLearningServiceComponent extends AccreditedPriorLearningServiceComponent {
	var accreditedPriorLearningService = Wire[AccreditedPriorLearningService]
}

trait AccreditedPriorLearningService {
	def getByNotionalKey(studentCourseDetails: Option[StudentCourseDetails],
											 award: Option[Award],
											 sequenceNumber: Integer): Option[AccreditedPriorLearning]}

abstract class AbstractAccreditedPriorLearningService extends AccreditedPriorLearningService {
	self: AccreditedPriorLearningDaoComponent =>

	def getByNotionalKey(studentCourseDetails: Option[StudentCourseDetails],
																						award: Option[Award],
																						sequenceNumber: Integer): Option[AccreditedPriorLearning] = {
		(studentCourseDetails, award, sequenceNumber) match {
			case (Some(scd: StudentCourseDetails), Some(awd: Award), sequence: Integer) =>
				accreditedPriorLearningDao.getByNotionalKey(scd, awd, sequence)
			case _ => None
		}
	}
}

@Service ("accreditedPriorLearningService")
class AccreditedPriorLearningServiceImpl extends AbstractAccreditedPriorLearningService
	with AutowiringAccreditedPriorLearningDaoComponent
