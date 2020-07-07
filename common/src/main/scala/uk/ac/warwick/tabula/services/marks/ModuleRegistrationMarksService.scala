package uk.ac.warwick.tabula.services.marks

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{MarkState, ModuleRegistration, RecordedModuleRegistration}
import uk.ac.warwick.tabula.data.{AutowiringModuleRegistrationMarksDaoComponent, AutowiringTransactionalComponent, ModuleRegistrationMarksDaoComponent, TransactionalComponent}

trait ModuleRegistrationMarksService {
  def getOrCreateRecordedModuleRegistration(reg: ModuleRegistration): RecordedModuleRegistration
  def getRecordedModuleRegistration(reg: ModuleRegistration): Option[RecordedModuleRegistration]
  def getAllRecordedModuleRegistrations(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): Seq[RecordedModuleRegistration]
  def allNeedingWritingToSits(filtered: Boolean): Seq[RecordedModuleRegistration]
  def mostRecentlyWrittenToSitsDate: Option[DateTime]
  def saveOrUpdate(reg: RecordedModuleRegistration): RecordedModuleRegistration
}

abstract class AbstractModuleRegistrationMarksService extends ModuleRegistrationMarksService {
  self: ModuleRegistrationMarksDaoComponent
    with TransactionalComponent =>

  override def getRecordedModuleRegistration(reg: ModuleRegistration): Option[RecordedModuleRegistration] = transactional(readOnly = true) {
    moduleRegistrationMarksDao.getRecordedModuleRegistration(reg)
  }

  override def getOrCreateRecordedModuleRegistration(reg: ModuleRegistration): RecordedModuleRegistration = transactional(readOnly = true) {
    moduleRegistrationMarksDao.getRecordedModuleRegistration(reg)
      .getOrElse(new RecordedModuleRegistration(reg))
  }

  override def getAllRecordedModuleRegistrations(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): Seq[RecordedModuleRegistration] = transactional(readOnly = true) {
    moduleRegistrationMarksDao.getAllRecordedModuleRegistrations(sitsModuleCode, academicYear, occurrence)
  }

  override def allNeedingWritingToSits(filtered: Boolean): Seq[RecordedModuleRegistration] = transactional(readOnly = true) {
    if (filtered) {
      val allModuleMarksNeedsWritingToSits = moduleRegistrationMarksDao.allNeedingWritingToSits.filterNot(_.marks.isEmpty)

      val moduleMarksCanUploadToSitsForYear =
        allModuleMarksNeedsWritingToSits.filter { student =>
          student.moduleRegistration.map(_.module).exists(m => m.adminDepartment.canUploadMarksToSitsForYear(student.academicYear, m))
        }

      moduleMarksCanUploadToSitsForYear.filter { student =>
        // true if latestState is empty (which should never be the case anyway)
        student.latestState.forall { markState =>
          markState != MarkState.Agreed || student.moduleRegistration.exists { moduleRegistration =>
            MarkState.resultsReleasedToStudents(student.academicYear, Option(moduleRegistration.studentCourseDetails))
          }
        }
      }
    } else {
      moduleRegistrationMarksDao.allNeedingWritingToSits
    }
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
