package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.StudentAward
import uk.ac.warwick.tabula.data.{AutowiringStudentAwardDaoComponent, AutowiringTransactionalComponent, StudentAwardDaoComponent, TransactionalComponent}

trait StudentAwardService {
  def saveOrUpdate(sa: StudentAward): Unit
  def delete(sa: StudentAward): Unit
  def getByAcademicYears(academicYears: Seq[AcademicYear]): Seq[StudentAward]
  def getByUniversityIds(universityIds: Seq[String]): Seq[StudentAward]
  def getBySprCodeAndAcademicYear(sprCode: String, academicYear:AcademicYear): Seq[StudentAward]
}

abstract class DatabaseStudentAwardService extends StudentAwardService {
  self: StudentAwardDaoComponent
    with TransactionalComponent =>

  override def saveOrUpdate(sa: StudentAward): Unit = transactional() {
    studentAwardDao.saveOrUpdate(sa)
  }

  override def delete(sa: StudentAward): Unit = transactional() {
    studentAwardDao.delete(sa)
  }

  override def getByAcademicYears(academicYears: Seq[AcademicYear]): Seq[StudentAward] = transactional(readOnly = true) {
    studentAwardDao.getByAcademicYears(academicYears)
  }

  override def getByUniversityIds(universityIds: Seq[String]): Seq[StudentAward] = transactional(readOnly = true) {
    studentAwardDao.getByUniversityIds(universityIds)
  }

  override def getBySprCodeAndAcademicYear(sprCode: String, academicYear: AcademicYear): Seq[StudentAward]  = transactional(readOnly = true) {
    studentAwardDao.getBySprCodeAndAcademicYear(sprCode, academicYear)
  }
}

@Service("studentAwardService")
class AutowiringStudentAwardService
  extends DatabaseStudentAwardService
    with AutowiringStudentAwardDaoComponent
    with AutowiringTransactionalComponent

trait StudentAwardServiceComponent {
  def studentAwardService: StudentAwardService
}

trait AutowiringStudentAwardServiceComponent extends StudentAwardServiceComponent {
  var studentAwardService: StudentAwardService = Wire[StudentAwardService]
}

