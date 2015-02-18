package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Exam
import uk.ac.warwick.tabula.data.{ExamDaoComponent, AutowiringExamDaoComponent}


trait ExamServiceComponent {
	def examService: ExamService
}

trait AutowiringExamServiceComponent extends ExamServiceComponent {
	val examService = Wire[ExamService]
}

trait ExamService {
	def saveOrUpdate(exam: Exam): Unit
}

abstract class AbstractExamService extends ExamService {

	self: ExamDaoComponent =>

	def saveOrUpdate(exam: Exam): Unit =
		examDao.saveOrUpdate(exam)
}


@Service("examService")
class ExamServiceImpl
	extends AbstractExamService
	with AutowiringExamDaoComponent
