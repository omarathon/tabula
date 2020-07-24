package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Projections._
import org.hibernate.criterion.Restrictions._
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.Daoisms._
import uk.ac.warwick.tabula.data.model.{ModuleRegistration, RecordedModuleRegistration}

trait ModuleRegistrationMarksDao {
  def getRecordedModuleRegistration(reg: ModuleRegistration): Option[RecordedModuleRegistration]
  def getAllRecordedModuleRegistrations(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): Seq[RecordedModuleRegistration]
  def getAllForModulesInYear(moduleCodes: Seq[String], academicYear: AcademicYear): Seq[RecordedModuleRegistration]
  def allNeedingWritingToSits: Seq[RecordedModuleRegistration]
  def mostRecentlyWrittenToSitsDate: Option[DateTime]
  def saveOrUpdate(reg: RecordedModuleRegistration): RecordedModuleRegistration
}

abstract class AbstractModuleRegistrationMarksDao extends ModuleRegistrationMarksDao {
  self: ExtendedSessionComponent
    with HelperRestrictions =>

  override def getRecordedModuleRegistration(reg: ModuleRegistration): Option[RecordedModuleRegistration] =
    session.newCriteria[RecordedModuleRegistration]
      .add(is("sprCode", reg.sprCode))
      .add(is("sitsModuleCode", reg.sitsModuleCode))
      .add(is("academicYear", reg.academicYear))
      .add(is("occurrence", reg.occurrence))
      .uniqueResult

  override def getAllRecordedModuleRegistrations(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): Seq[RecordedModuleRegistration] =
    session.newCriteria[RecordedModuleRegistration]
      .setFetchMode("_marks", FetchMode.JOIN)
      .add(is("sitsModuleCode", sitsModuleCode))
      .add(is("academicYear", academicYear))
      .add(is("occurrence", occurrence))
      .distinct
      .seq

  override def getAllForModulesInYear(moduleCodes: Seq[String], academicYear: AcademicYear): Seq[RecordedModuleRegistration] =
    safeInSeq(
      () =>
        session.newCriteria[RecordedModuleRegistration]
          .setFetchMode("_marks", FetchMode.JOIN)
          .add(is("academicYear", academicYear)),
      "sitsModuleCode",
      moduleCodes
    )

  override def allNeedingWritingToSits: Seq[RecordedModuleRegistration] =
    session.newCriteria[RecordedModuleRegistration]
      .add(isNotNull("_needsWritingToSitsSince"))
      .distinct
      .seq

  override def mostRecentlyWrittenToSitsDate: Option[DateTime] =
    session.newCriteria[RecordedModuleRegistration]
      .add(isNotNull("_lastWrittenToSits"))
      .project[DateTime](max("_lastWrittenToSits"))
      .uniqueResult

  override def saveOrUpdate(reg: RecordedModuleRegistration): RecordedModuleRegistration = {
    session.saveOrUpdate(reg)
    reg
  }
}

@Repository
class AutowiringModuleRegistrationMarksDao
  extends AbstractModuleRegistrationMarksDao
    with Daoisms

trait ModuleRegistrationMarksDaoComponent {
  def moduleRegistrationMarksDao: ModuleRegistrationMarksDao
}

trait AutowiringModuleRegistrationMarksDaoComponent extends ModuleRegistrationMarksDaoComponent {
  var moduleRegistrationMarksDao: ModuleRegistrationMarksDao = Wire[ModuleRegistrationMarksDao]
}
