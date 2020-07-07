package uk.ac.warwick.tabula.services.marks

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AssessmentComponentMarksDaoComponent, AutowiringAssessmentComponentMarksDaoComponent, AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringModuleRegistrationServiceComponent, ModuleAndDepartmentServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, SprCode}

trait AssessmentComponentMarksService {
  def getRecordedStudent(uagm: UpstreamAssessmentGroupMember): Option[RecordedAssessmentComponentStudent]
  def getOrCreateRecordedStudent(uagm: UpstreamAssessmentGroupMember): RecordedAssessmentComponentStudent
  def getAllRecordedStudents(uag: UpstreamAssessmentGroup): Seq[RecordedAssessmentComponentStudent]
  def allNeedingWritingToSits(filtered: Boolean): Seq[RecordedAssessmentComponentStudent]
  def mostRecentlyWrittenStudentDate: Option[DateTime]
  def saveOrUpdate(student: RecordedAssessmentComponentStudent): RecordedAssessmentComponentStudent
}

abstract class AbstractAssessmentComponentMarksService extends AssessmentComponentMarksService {
  self: AssessmentComponentMarksDaoComponent
    with ModuleAndDepartmentServiceComponent
    with ModuleRegistrationServiceComponent
    with TransactionalComponent =>

  override def getRecordedStudent(uagm: UpstreamAssessmentGroupMember): Option[RecordedAssessmentComponentStudent] = transactional(readOnly = true) {
    assessmentComponentMarksDao.getRecordedStudent(uagm)
  }

  override def getOrCreateRecordedStudent(uagm: UpstreamAssessmentGroupMember): RecordedAssessmentComponentStudent = transactional(readOnly = true) {
    assessmentComponentMarksDao.getRecordedStudent(uagm)
      .getOrElse(new RecordedAssessmentComponentStudent(uagm))
  }

  override def getAllRecordedStudents(uag: UpstreamAssessmentGroup): Seq[RecordedAssessmentComponentStudent] = transactional(readOnly = true) {
    assessmentComponentMarksDao.getAllRecordedStudents(uag)
  }

  override def allNeedingWritingToSits(filtered: Boolean): Seq[RecordedAssessmentComponentStudent] = transactional(readOnly = true) {
    if (filtered) {
      val allComponentMarksNeedsWritingToSits = assessmentComponentMarksDao.allNeedingWritingToSits.filterNot(_.marks.isEmpty)

      type UniversityId = String
      type TabulaModuleCode = String
      type SitsModuleCode = String
      type Occurrence = String

      val allModules: Map[TabulaModuleCode, Module] =
        moduleAndDepartmentService.getModulesByCodes(
          allComponentMarksNeedsWritingToSits.map(_.moduleCode).distinct.flatMap(Module.stripCats).map(_.toLowerCase)
        ).map(module => module.code -> module).toMap

      val componentMarksCanUploadToSitsForYear =
        allComponentMarksNeedsWritingToSits.filter { student =>
          Module.stripCats(student.moduleCode).map(_.toLowerCase).flatMap(allModules.get).forall { module =>
            module.adminDepartment.canUploadMarksToSitsForYear(student.academicYear, module)
          }
        }

      val allModuleRegistrations: Map[UniversityId, Map[(SitsModuleCode, AcademicYear, Occurrence), Seq[ModuleRegistration]]] =
        moduleRegistrationService.getByUniversityIds(componentMarksCanUploadToSitsForYear.map(_.universityId).distinct, includeDeleted = false)
          .groupBy(mr => SprCode.getUniversityId(mr.sprCode))
          .map { case (sprCode, registrations) =>
            sprCode -> registrations.groupBy(mr => (mr.sitsModuleCode, mr.academicYear, mr.occurrence))
          }

      componentMarksCanUploadToSitsForYear
        .map { student =>
          // We can't restrict this by AssessmentGroup because it might be a resit mark by another mechanism
          lazy val moduleRegistrations: Seq[ModuleRegistration] =
            allModuleRegistrations
              .getOrElse(student.universityId, Map.empty)
              .getOrElse((student.moduleCode, student.academicYear, student.occurrence), Seq.empty)

          student -> moduleRegistrations
        }
        .filter { case (student, moduleRegistrations) =>
          // true if latestState is empty (which should never be the case anyway)
          student.latestState.forall { markState =>
            markState != MarkState.Agreed || moduleRegistrations.exists { moduleRegistration =>
              MarkState.resultsReleasedToStudents(student.academicYear, Option(moduleRegistration.studentCourseDetails), MarkState.MarkUploadTime)
            }
          }
        }
        .map(_._1)
    } else {
      assessmentComponentMarksDao.allNeedingWritingToSits
    }
  }

  override def mostRecentlyWrittenStudentDate: Option[DateTime] = transactional(readOnly = true) {
    assessmentComponentMarksDao.mostRecentlyWrittenStudentDate
  }

  override def saveOrUpdate(student: RecordedAssessmentComponentStudent): RecordedAssessmentComponentStudent = transactional() {
    assessmentComponentMarksDao.saveOrUpdate(student)
  }
}

@Service("assessmentComponentMarksService")
class AutowiringAssessmentComponentMarksService
  extends AbstractAssessmentComponentMarksService
    with AutowiringAssessmentComponentMarksDaoComponent
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringModuleRegistrationServiceComponent
    with AutowiringTransactionalComponent

trait AssessmentComponentMarksServiceComponent {
  def assessmentComponentMarksService: AssessmentComponentMarksService
}

trait AutowiringAssessmentComponentMarksServiceComponent extends AssessmentComponentMarksServiceComponent {
  var assessmentComponentMarksService: AssessmentComponentMarksService = Wire[AssessmentComponentMarksService]
}
