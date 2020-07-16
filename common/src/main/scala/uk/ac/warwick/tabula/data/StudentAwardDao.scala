package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Order._
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.Daoisms._
import uk.ac.warwick.tabula.data.model.StudentAward

trait StudentAwardDao {
  def saveOrUpdate(sa: StudentAward): Unit
  def delete(sa: StudentAward): Unit
  def getByAcademicYears(academicYears: Seq[AcademicYear]): Seq[StudentAward]
  def getByUniversityIds(universityIds: Seq[String]): Seq[StudentAward]
}

abstract class HibernateStudentAwardDao extends StudentAwardDao with HelperRestrictions {
  self: ExtendedSessionComponent =>

  override def saveOrUpdate(sa: StudentAward): Unit = session.saveOrUpdate(sa)

  override def delete(sa: StudentAward): Unit = session.delete(sa)

  override def getByAcademicYears(academicYears: Seq[AcademicYear]): Seq[StudentAward] =
    safeInSeq(() => {
      session.newCriteria[StudentAward]
        .addOrder(asc("sprCode"))
        .addOrder(asc("award"))
    }, "academicYear", academicYears)

  override def getByUniversityIds(universityIds: Seq[String]): Seq[StudentAward] = {
    session.newQuery[StudentAward](
      """
        select distinct sa
        from StudentAward sa
        join StudentCourseDetails studentCourseDetails
          on studentCourseDetails.sprCode = sa.sprCode
        where studentCourseDetails.missingFromImportSince is null
          and studentCourseDetails.student.universityId in :universityIds
      """)
      .setParameterList("universityIds", universityIds)
      .seq
  }
}

@Repository("studentAwardDao")
class AutowiringStudentAwardDao
  extends HibernateStudentAwardDao
    with Daoisms

trait StudentAwardDaoComponent {
  def studentAwardDao: StudentAwardDao
}

trait AutowiringStudentAwardDaoComponent extends StudentAwardDaoComponent {
  var studentAwardDao: StudentAwardDao = Wire[StudentAwardDao]
}
