package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Daoisms._
import uk.ac.warwick.tabula.data.model.{RecordedAssessmentComponentStudent, UpstreamAssessmentGroup, UpstreamAssessmentGroupMember}

trait AssessmentComponentMarksDao {
  def getRecordedStudent(uagm: UpstreamAssessmentGroupMember): Option[RecordedAssessmentComponentStudent]
  def getAllRecordedStudents(uag: UpstreamAssessmentGroup): Seq[RecordedAssessmentComponentStudent]
  def allNeedingWritingToSits: Seq[RecordedAssessmentComponentStudent]
  def saveOrUpdate(student: RecordedAssessmentComponentStudent): RecordedAssessmentComponentStudent
}

abstract class AbstractAssessmentComponentMarksDao extends AssessmentComponentMarksDao {
  self: ExtendedSessionComponent
    with HelperRestrictions =>

  override def getRecordedStudent(uagm: UpstreamAssessmentGroupMember): Option[RecordedAssessmentComponentStudent] =
    session.newCriteria[RecordedAssessmentComponentStudent]
      .add(is("moduleCode", uagm.upstreamAssessmentGroup.moduleCode))
      .add(is("assessmentGroup", uagm.upstreamAssessmentGroup.assessmentGroup))
      .add(is("occurrence", uagm.upstreamAssessmentGroup.occurrence))
      .add(is("sequence", uagm.upstreamAssessmentGroup.sequence))
      .add(is("academicYear", uagm.upstreamAssessmentGroup.academicYear))
      .add(is("universityId", uagm.universityId))
      .uniqueResult

  override def getAllRecordedStudents(uag: UpstreamAssessmentGroup): Seq[RecordedAssessmentComponentStudent] =
    session.newCriteria[RecordedAssessmentComponentStudent]
      .add(is("moduleCode", uag.moduleCode))
      .add(is("assessmentGroup", uag.assessmentGroup))
      .add(is("occurrence", uag.occurrence))
      .add(is("sequence", uag.sequence))
      .add(is("academicYear", uag.academicYear))
      .seq

  override def allNeedingWritingToSits: Seq[RecordedAssessmentComponentStudent] =
    session.newCriteria[RecordedAssessmentComponentStudent]
      .add(is("needsWritingToSits", true))
      // TODO order by most recent mark updatedDate asc
      .seq

  override def saveOrUpdate(student: RecordedAssessmentComponentStudent): RecordedAssessmentComponentStudent = {
    session.saveOrUpdate(student)
    student
  }
}

@Repository
class AutowiringAssessmentComponentMarksDao
  extends AbstractAssessmentComponentMarksDao
    with Daoisms

trait AssessmentComponentMarksDaoComponent {
  def assessmentComponentMarksDao: AssessmentComponentMarksDao
}

trait AutowiringAssessmentComponentMarksDaoComponent extends AssessmentComponentMarksDaoComponent {
  var assessmentComponentMarksDao: AssessmentComponentMarksDao = Wire[AssessmentComponentMarksDao]
}
