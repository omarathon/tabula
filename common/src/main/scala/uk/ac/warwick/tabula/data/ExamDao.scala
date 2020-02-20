package uk.ac.warwick.tabula.data

import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._

trait ExamDaoComponent {
  val examDao: ExamDao
}

trait AutowiringExamDaoComponent extends ExamDaoComponent {
  val examDao: ExamDao = Wire[ExamDao]
}

trait ExamDao {
  def getById(id: String): Option[Exam]
  def save(exam: Exam): Unit
}

@Repository
class ExamDaoImpl extends ExamDao with Daoisms {

  override def getById(id: String): Option[Exam] = getById[Exam](id)

  override def save(exam: Exam): Unit = {
    exam.lastModified = DateTime.now()
    session.saveOrUpdate(exam)
  }
}
