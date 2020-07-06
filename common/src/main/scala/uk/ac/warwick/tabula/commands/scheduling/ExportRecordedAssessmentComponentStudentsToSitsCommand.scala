package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.ExportRecordedAssessmentComponentStudentsToSitsCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportFeedbackToSitsServiceComponent, ExportFeedbackToSitsServiceComponent}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object ExportRecordedAssessmentComponentStudentsToSitsCommand {
  type Result = Seq[RecordedAssessmentComponentStudent]
  type Command = Appliable[Result]

  def apply(): Command =
    new ExportRecordedAssessmentComponentStudentsToSitsCommandInternal()
      with ComposableCommand[Result]
      with ExportRecordedAssessmentComponentStudentsToSitsPermissions
      with ExportRecordedAssessmentComponentStudentsToSitsDescription
      with AutowiringExportFeedbackToSitsServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringTransactionalComponent
}

abstract class ExportRecordedAssessmentComponentStudentsToSitsCommandInternal
  extends CommandInternal[Result]
    with Logging {
  self: ExportFeedbackToSitsServiceComponent
    with AssessmentComponentMarksServiceComponent
    with AssessmentMembershipServiceComponent
    with ModuleAndDepartmentServiceComponent
    with ModuleRegistrationServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional() {
    val marksToUpload =
      assessmentComponentMarksService.allNeedingWritingToSits
        .filterNot(_.marks.isEmpty) // Should never happen anyway
        .filterNot { student =>
          lazy val canUploadMarksToSitsForYear =
            moduleAndDepartmentService.getModuleBySitsCode(student.moduleCode).forall { module =>
              module.adminDepartment.canUploadMarksToSitsForYear(student.academicYear, module)
            }

          // We can't restrict this by AssessmentGroup because it might be a resit mark by another mechanism
          lazy val moduleRegistrations: Seq[ModuleRegistration] =
            moduleRegistrationService.getByModuleOccurrence(student.moduleCode, student.academicYear, student.occurrence)
              .filter(_.studentCourseDetails.student.universityId == student.universityId)

          lazy val canUploadMarksToSits: Boolean = {
            // true if latestState is empty (which should never be the case anyway)
            student.latestState.forall { markState =>
              markState != MarkState.Agreed || moduleRegistrations.exists { moduleRegistration =>
                MarkState.resultsReleasedToStudents(student.academicYear, Option(moduleRegistration.studentCourseDetails))
              }
            }
          }

          !canUploadMarksToSitsForYear || !canUploadMarksToSits
        }
        .sortBy(_.marks.head.updatedDate).reverse // Upload most recently updated first (so a stuck queue doesn't prevent upload)
        .take(1000) // Don't try and upload more than 1000 at a time or we end up with too big a transaction

    marksToUpload.flatMap { student =>
      lazy val upstreamAssessmentGroupMember: Option[UpstreamAssessmentGroupMember] =
        assessmentMembershipService.getUpstreamAssessmentGroup(new UpstreamAssessmentGroup {
          this.academicYear = student.academicYear
          this.occurrence = student.occurrence
          this.moduleCode = student.moduleCode
          this.sequence = student.sequence
          this.assessmentGroup = student.assessmentGroup
        }).flatMap(_.members.asScala.find(uagm => uagm.universityId == student.universityId && uagm.assessmentType == student.assessmentType))

      // first check to see if there is one and only one matching row
      exportFeedbackToSitsService.countMatchingSitsRecords(student) match {
        case 0 =>
          logger.warn(s"Not updating SITS for assessment component mark $student - found zero rows")
          None

        case r if r > 1 =>
          logger.warn(f"Not updating SITS for assessment component mark $student - found multiple rows")
          None

        case _ =>
          // update - expecting to update one row
          exportFeedbackToSitsService.exportToSits(student) match {
            case 0 =>
              logger.warn(s"Upload to SITS for assessment component mark $student failed - found zero rows")
              None

            case r if r > 1 =>
              throw new IllegalStateException(s"Unexpected SITS update! Only expected to update one row, but $r rows were updated for assessment component mark $student")

            case 1 =>
              student.needsWritingToSits = false
              student.lastWrittenToSits = Some(DateTime.now)

              // Also update the UpstreamAssessmentGroupMember record so it doesn't show as out of sync
              upstreamAssessmentGroupMember.foreach { uagm =>
                uagm.actualMark = student.latestMark
                uagm.actualGrade = student.latestGrade
                uagm.agreedMark = student.latestMark.filter(_ => student.latestState.contains(MarkState.Agreed))
                uagm.agreedGrade = student.latestGrade.filter(_ => student.latestState.contains(MarkState.Agreed))

                assessmentMembershipService.save(uagm)
              }

              Some(assessmentComponentMarksService.saveOrUpdate(student))
          }
      }
    }
  }
}

trait ExportRecordedAssessmentComponentStudentsToSitsPermissions extends RequiresPermissionsChecking {
  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Marks.UploadToSits)
  }
}

trait ExportRecordedAssessmentComponentStudentsToSitsDescription extends Describable[Result] {
  override lazy val eventName: String = "ExportRecordedAssessmentComponentStudentsToSits"

  override def describe(d: Description): Unit = {}

  override def describeResult(d: Description, result: Result): Unit =
    d.properties(
      "marks" -> result.filter(_.latestMark.nonEmpty).map { student =>
        student.universityId -> student.latestMark.get
      }.toMap,
      "grades" -> result.filter(_.latestGrade.nonEmpty).map { student =>
        student.universityId -> student.latestGrade.get
      }.toMap
    )
}
