package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Projections._
import org.hibernate.criterion.Restrictions._
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.Daoisms._
import uk.ac.warwick.tabula.data.model.{Module, ModuleRegistration, RecordedModuleRegistration}

trait ModuleRegistrationMarksDao {
  def getRecordedModuleRegistration(reg: ModuleRegistration): Option[RecordedModuleRegistration]
  def getAllRecordedModuleRegistrations(module: Module, cats: BigDecimal, academicYear: AcademicYear, occurrence: String): Seq[RecordedModuleRegistration]
  def allNeedingWritingToSits: Seq[RecordedModuleRegistration]
  def mostRecentlyWrittenToSitsDate: Option[DateTime]
  def saveOrUpdate(reg: RecordedModuleRegistration): RecordedModuleRegistration
}

abstract class AbstractModuleRegistrationMarksDao extends ModuleRegistrationMarksDao {
  self: ExtendedSessionComponent
    with HelperRestrictions =>

  override def getRecordedModuleRegistration(reg: ModuleRegistration): Option[RecordedModuleRegistration] =
    session.newCriteria[RecordedModuleRegistration]
      .add(is("scjCode", reg._scjCode))
      .add(is("module", reg.module))
      .add(is("cats", reg.cats))
      .add(is("academicYear", reg.academicYear))
      .add(is("occurrence", reg.occurrence))
      .uniqueResult

  override def getAllRecordedModuleRegistrations(module: Module, cats: BigDecimal, academicYear: AcademicYear, occurrence: String): Seq[RecordedModuleRegistration] =
    session.newCriteria[RecordedModuleRegistration]
      .setFetchMode("_marks", FetchMode.JOIN)
      .add(is("module", module))
      .add(is("cats", JBigDecimal(Some(cats))))
      .add(is("academicYear", academicYear))
      .add(is("occurrence", occurrence))
      .distinct
      .seq

  override def allNeedingWritingToSits: Seq[RecordedModuleRegistration] =
    session.newCriteria[RecordedModuleRegistration]
      .add(is("needsWritingToSits", true))
      // TODO order by most recent mark updatedDate asc
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
