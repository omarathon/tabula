package uk.ac.warwick.tabula.services.marks

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{Module, ModuleRegistration, RecordedModuleRegistration}
import uk.ac.warwick.tabula.data.{AutowiringModuleRegistrationMarksDaoComponent, AutowiringTransactionalComponent, ModuleRegistrationMarksDaoComponent, TransactionalComponent}

trait ModuleRegistrationMarksService {
  def getOrCreateRecordedModuleRegistration(reg: ModuleRegistration): RecordedModuleRegistration
  def getAllRecordedModuleRegistrations(module: Module, cats: BigDecimal, academicYear: AcademicYear, occurrence: String): Seq[RecordedModuleRegistration]
  def allNeedingWritingToSits: Seq[RecordedModuleRegistration]
  def mostRecentlyWrittenToSitsDate: Option[DateTime]
  def saveOrUpdate(reg: RecordedModuleRegistration): RecordedModuleRegistration
}

abstract class AbstractModuleRegistrationMarksService extends ModuleRegistrationMarksService {
  self: ModuleRegistrationMarksDaoComponent
    with TransactionalComponent =>

  override def getOrCreateRecordedModuleRegistration(reg: ModuleRegistration): RecordedModuleRegistration = transactional(readOnly = true) {
    moduleRegistrationMarksDao.getRecordedModuleRegistration(reg)
      .getOrElse(new RecordedModuleRegistration(reg))
  }

  override def getAllRecordedModuleRegistrations(module: Module, cats: BigDecimal, academicYear: AcademicYear, occurrence: String): Seq[RecordedModuleRegistration] = transactional(readOnly = true) {
    moduleRegistrationMarksDao.getAllRecordedModuleRegistrations(module, cats, academicYear, occurrence)
  }

  override def allNeedingWritingToSits: Seq[RecordedModuleRegistration] = transactional(readOnly = true) {
    moduleRegistrationMarksDao.allNeedingWritingToSits
  }

  override def mostRecentlyWrittenToSitsDate: Option[DateTime] = transactional(readOnly = true) {
    moduleRegistrationMarksDao.mostRecentlyWrittenToSitsDate
  }

  override def saveOrUpdate(reg: RecordedModuleRegistration): RecordedModuleRegistration = transactional() {
    moduleRegistrationMarksDao.saveOrUpdate(reg)
  }
}

@Service("moduleRegistrationMarksService")
class AutowiringModuleRegistrationMarksService
  extends AbstractModuleRegistrationMarksService
    with AutowiringModuleRegistrationMarksDaoComponent
    with AutowiringTransactionalComponent

trait ModuleRegistrationMarksServiceComponent {
  def moduleRegistrationMarksService: ModuleRegistrationMarksService
}

trait AutowiringModuleRegistrationMarksServiceComponent extends ModuleRegistrationMarksServiceComponent {
  var moduleRegistrationMarksService: ModuleRegistrationMarksService = Wire[ModuleRegistrationMarksService]
}
