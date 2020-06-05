package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.ExportRecordedModuleRegistrationsToSitsCommand._
import uk.ac.warwick.tabula.data.model.RecordedModuleRegistration
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.marks.{AutowiringModuleRegistrationMarksServiceComponent, ModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportStudentModuleResultToSitsServiceComponent, ExportStudentModuleResultToSitsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

object ExportRecordedModuleRegistrationsToSitsCommand {
  type Result = Seq[RecordedModuleRegistration]
  type Command = Appliable[Result]

  def apply(): Command =
    new ExportRecordedModuleRegistrationsToSitsCommandInternal()
      with ComposableCommand[Result]
      with ExportRecordedModuleRegistrationsToSitsCommandPermissions
      with ExportRecordedModuleRegistrationsToSitsDescription
      with AutowiringExportStudentModuleResultToSitsServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringTransactionalComponent
}

abstract class ExportRecordedModuleRegistrationsToSitsCommandInternal
  extends CommandInternal[Result]
    with Logging {
  self: ExportStudentModuleResultToSitsServiceComponent
    with ModuleRegistrationMarksServiceComponent
    with ModuleAndDepartmentServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional() {
    val moduleMarksToUpload = moduleRegistrationMarksService.allNeedingWritingToSits

    moduleMarksToUpload.flatMap { student =>
      val canUploadMarksToSitsForYear = student.moduleRegistration.map(_.module).exists(m => m.adminDepartment.canUploadMarksToSitsForYear(student.academicYear, m))

      if (!canUploadMarksToSitsForYear) {
        logger.warn(s"Not uploading module mark $student as department for ${student.sitsModuleCode} is closed for ${student.academicYear}")
        None
      } else {
        exportStudentModuleResultToSitsService.exportModuleMarksToSits(student) match {
          case r if r > 1 =>
            throw new IllegalStateException(s"Unexpected SITS SMR update! Only expected to update one row, but $r rows were updated for module mark $student")
          case 1 =>
            student.needsWritingToSits = false
            student.lastWrittenToSits = Some(DateTime.now)
            Some(moduleRegistrationMarksService.saveOrUpdate(student))
          case _ =>
            None
        }
      }
    }
  }
}

trait ExportRecordedModuleRegistrationsToSitsCommandPermissions extends RequiresPermissionsChecking {
  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Marks.UploadToSits)
  }
}

trait ExportRecordedModuleRegistrationsToSitsDescription extends Describable[Result] {
  override lazy val eventName: String = "ExportRecordedModuleRegistrationsToSits"

  override def describe(d: Description): Unit = {}

  override def describeResult(d: Description, result: Result): Unit =
    d.properties(
      "marks" -> result.filter(_.latestMark.nonEmpty).map { student =>
        student.sprCode -> student.latestMark.get
      }.toMap,
      "grades" -> result.filter(_.latestGrade.nonEmpty).map { student =>
        student.sprCode -> student.latestGrade.get
      }.toMap
    )
}
