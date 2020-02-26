package uk.ac.warwick.tabula.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{Department, Exam}
import uk.ac.warwick.tabula.data.{Daoisms, ExamDao}
import uk.ac.warwick.tabula.helpers.Logging

trait ExamService {
  def save(exam: Exam): Exam
  def getExamsByDepartment(department: Department, academicYear: AcademicYear): Seq[Exam]
}

@Service(value = "examService")
class ExamServiceImpl extends ExamService with Daoisms with Logging {
  @Autowired var dao: ExamDao = _

  def save(exam: Exam): Exam = dao.save(exam)

  def getExamsByDepartment(department: Department, academicYear: AcademicYear): Seq[Exam] =
    dao.getExamsByDepartment(department, academicYear)

}

trait ExamServiceComponent {
  def examService: ExamService
}

trait AutowiringExamServiceComponent extends ExamServiceComponent {
  var examService: ExamService = Wire[ExamService]
}
