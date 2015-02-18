package uk.ac.warwick.tabula.data

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import org.springframework.stereotype.Repository

trait ExamDaoComponent {
	val examDao: ExamDao
}

trait AutowiringExamDaoComponent extends ExamDaoComponent {
	val examDao = Wire[ExamDao]
}

trait ExamDao {

	def saveOrUpdate(exam: Exam): Unit

}

@Repository
class ExamDaoImpl extends ExamDao with Daoisms {

	def saveOrUpdate(exam: Exam) = session.saveOrUpdate(exam)

}
