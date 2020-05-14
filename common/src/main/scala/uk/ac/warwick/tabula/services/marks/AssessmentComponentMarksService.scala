package uk.ac.warwick.tabula.services.marks

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{RecordedAssessmentComponentStudent, UpstreamAssessmentGroup, UpstreamAssessmentGroupMember}
import uk.ac.warwick.tabula.data.{AssessmentComponentMarksDaoComponent, AutowiringAssessmentComponentMarksDaoComponent, AutowiringTransactionalComponent, TransactionalComponent}

trait AssessmentComponentMarksService {
  def getOrCreateRecordedStudent(uagm: UpstreamAssessmentGroupMember): RecordedAssessmentComponentStudent
  def getAllRecordedStudents(uag: UpstreamAssessmentGroup): Seq[RecordedAssessmentComponentStudent]
  def allNeedingWritingToSits: Seq[RecordedAssessmentComponentStudent]
  def saveOrUpdate(student: RecordedAssessmentComponentStudent): RecordedAssessmentComponentStudent
}

abstract class AbstractAssessmentComponentMarksService extends AssessmentComponentMarksService {
  self: AssessmentComponentMarksDaoComponent
    with TransactionalComponent =>

  override def getOrCreateRecordedStudent(uagm: UpstreamAssessmentGroupMember): RecordedAssessmentComponentStudent = transactional(readOnly = true) {
    assessmentComponentMarksDao.getRecordedStudent(uagm)
      .getOrElse(new RecordedAssessmentComponentStudent(uagm))
  }

  override def getAllRecordedStudents(uag: UpstreamAssessmentGroup): Seq[RecordedAssessmentComponentStudent] = transactional(readOnly = true) {
    assessmentComponentMarksDao.getAllRecordedStudents(uag)
  }

  override def allNeedingWritingToSits: Seq[RecordedAssessmentComponentStudent] = transactional(readOnly = true) {
    assessmentComponentMarksDao.allNeedingWritingToSits
  }

  override def saveOrUpdate(student: RecordedAssessmentComponentStudent): RecordedAssessmentComponentStudent = transactional() {
    assessmentComponentMarksDao.saveOrUpdate(student)
  }
}

@Service("assessmentComponentMarksService")
class AutowiringAssessmentComponentMarksService
  extends AbstractAssessmentComponentMarksService
    with AutowiringAssessmentComponentMarksDaoComponent
    with AutowiringTransactionalComponent

trait AssessmentComponentMarksServiceComponent {
  def assessmentComponentMarksService: AssessmentComponentMarksService
}

trait AutowiringAssessmentComponentMarksServiceComponent extends AssessmentComponentMarksServiceComponent {
  var assessmentComponentMarksService: AssessmentComponentMarksService = Wire[AssessmentComponentMarksService]
}
