package uk.ac.warwick.tabula.data


import org.hibernate.criterion.Order
import org.hibernate.criterion.Projections.max
import org.hibernate.criterion.Restrictions.isNotNull
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{RecordedModuleRegistration, RecordedResit, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.data.Daoisms._

trait ResitDao {
  def saveOrUpdate(resit: RecordedResit): RecordedResit
  def getAllResits(uag: UpstreamAssessmentGroup): Seq[RecordedResit]
  def findResits(sprCodes: Seq[String]): Seq[RecordedResit]
  def allNeedingWritingToSits: Seq[RecordedResit]
  def mostRecentlyWrittenToSitsDate: Option[DateTime]
}

abstract class AbstractResitDao extends ResitDao {
  self: ExtendedSessionComponent
    with HelperRestrictions =>


  override def saveOrUpdate(resit: RecordedResit): RecordedResit = {
    session.saveOrUpdate(resit)
    resit
  }

  override def getAllResits(uag: UpstreamAssessmentGroup): Seq[RecordedResit] =
    session.newCriteria[RecordedResit]
      .add(is("moduleCode", uag.moduleCode))
      .add(is("occurrence", uag.occurrence))
      .add(is("sequence", uag.sequence))
      .add(is("academicYear", uag.academicYear))
      .distinct
      .seq

  override def findResits(sprCodes: Seq[String]): Seq[RecordedResit] =
    session.newCriteria[RecordedResit]
      .add(safeIn("sprCode", sprCodes))
      .seq

  override def allNeedingWritingToSits: Seq[RecordedResit] =
    session.newCriteria[RecordedResit]
      .add(is("needsWritingToSits", true))
      .addOrder(Order.asc("updatedDate"))
      .seq

  override def mostRecentlyWrittenToSitsDate: Option[DateTime] =
    session.newCriteria[RecordedResit]
      .add(isNotNull("_lastWrittenToSits"))
      .project[DateTime](max("_lastWrittenToSits"))
      .uniqueResult
}

@Repository
class AutowiringResitDao
  extends AbstractResitDao
    with Daoisms

trait ResitDaoComponent {
  def resitDao: ResitDao
}

trait AutowiringResitDaoComponent extends ResitDaoComponent {
  var resitDao: ResitDao = Wire[ResitDao]
}
