package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.ExportRecordedModuleRegistrationsToSitsCommand._
import uk.ac.warwick.tabula.data.model.{MarkState, RecordedModuleRegistration}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.marks.{AutowiringModuleRegistrationMarksServiceComponent, ModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportStudentModuleResultToSitsServiceComponent, ExportStudentModuleResultToSitsServiceComponent}
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
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringTransactionalComponent
}

abstract class ExportRecordedModuleRegistrationsToSitsCommandInternal
  extends CommandInternal[Result]
    with Logging {
  self: ExportStudentModuleResultToSitsServiceComponent
    with ModuleRegistrationMarksServiceComponent
    with ModuleRegistrationServiceComponent
    with ModuleAndDepartmentServiceComponent
    with AssessmentMembershipServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional() {
    val moduleMarksToUpload =
      moduleRegistrationMarksService.allNeedingWritingToSits(filtered = true)
        .sortBy(_.marks.head.updatedDate).reverse // Upload most recently updated first (so a stuck queue doesn't prevent upload)
        .take(1000) // Don't try and upload more than 1000 at a time or we end up with too big a transaction

    moduleMarksToUpload.flatMap { student =>
      // TAB-8438 we set that the student has attended the final assessment for a module if they have a non-0 component mark
      // TODO How do we handle where there's no component mark? (Currently will be false)
      // TODO What should we do if the final assessment was cancelled (i.e. has a grade of FM) or where the student has mitigation (grade M)? (Currently will be false)

      // Find the final assessment component for a module
      val finalAssessmentAttended =
        student.moduleRegistration.exists { moduleRegistration =>
          moduleRegistration.upstreamAssessmentGroupMembers
            .find(_.upstreamAssessmentGroup.assessmentComponent.exists(_.finalChronologicalAssessment))
            .exists { uagm =>
              if (moduleRegistration.passFail) uagm.firstDefinedGrade.contains("P")
              else uagm.firstDefinedMark.exists(_ > 0)
            }
        }

      exportStudentModuleResultToSitsService.exportModuleMarksToSits(student, finalAssessmentAttended) match {
        case r if r > 1 =>
          throw new IllegalStateException(s"Unexpected SITS SMR update! Only expected to update one row, but $r rows were updated for module mark $student")
        case 1 =>
          student.needsWritingToSitsSince = None
          student.lastWrittenToSits = Some(DateTime.now)

          // Update the ModuleRegistration so it doesn't show as out of sync
          student.moduleRegistration.foreach { moduleRegistration =>
            moduleRegistration.actualMark = student.latestMark
            moduleRegistration.actualGrade = student.latestGrade
            moduleRegistration.agreedMark = student.latestMark.filter(_ => student.latestState.contains(MarkState.Agreed))
            moduleRegistration.agreedGrade = student.latestGrade.filter(_ => student.latestState.contains(MarkState.Agreed))
            moduleRegistration.moduleResult = student.latestResult.orNull

            moduleRegistrationService.saveOrUpdate(moduleRegistration)
          }

          Some(moduleRegistrationMarksService.saveOrUpdate(student))
        case _ =>
          None
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
      }.toMap,
      "results" -> result.filter(_.latestResult.nonEmpty).map { student =>
        student.sprCode -> student.latestResult.get.entryName
      }.toMap,
      "state" -> result.filter(_.latestState.nonEmpty).map { student =>
        student.sprCode -> student.latestState.get.entryName
      }.toMap
    )
}
