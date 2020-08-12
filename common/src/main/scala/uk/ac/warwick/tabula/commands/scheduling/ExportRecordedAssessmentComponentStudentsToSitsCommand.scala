package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.ExportRecordedAssessmentComponentStudentsToSitsCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportFeedbackToSitsServiceComponent, ExportFeedbackToSitsServiceComponent}
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
      with AutowiringTransactionalComponent
}

abstract class ExportRecordedAssessmentComponentStudentsToSitsCommandInternal
  extends CommandInternal[Result]
    with Logging
    with TaskBenchmarking {
  self: ExportFeedbackToSitsServiceComponent
    with AssessmentComponentMarksServiceComponent
    with AssessmentMembershipServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional() {
    val marksToUpload = benchmarkTask("Get next batch of component marks to upload") {
      assessmentComponentMarksService.allNeedingWritingToSits(filtered = true)
        .sortBy(_.marks.head.updatedDate).reverse // Upload most recently updated first (so a stuck queue doesn't prevent upload)
        .take(1000) // Don't try and upload more than 1000 at a time or we end up with too big a transaction
    }

    // Commonly this will include lots of duplicate UpstreamAssessmentGroups, so just fetch them once
    type SitsModuleCode = String
    type Occurrence = String
    type AssessmentSequence = String
    type AssessmentGroupCode = String
    val upstreamAssessmentGroups: Map[(SitsModuleCode, AcademicYear, Occurrence, AssessmentSequence, AssessmentGroupCode), UpstreamAssessmentGroup] = benchmarkTask("Get all UAGs") {
      marksToUpload.map(student => (student.moduleCode, student.academicYear, student.occurrence, student.sequence, student.assessmentGroup))
        .distinct
        .flatMap { case (studentSitsModuleCode, studentAcademicYear, studentOccurrence, studentAssessmentSequence, studentAssessmentGroupCode) =>
          assessmentMembershipService.getUpstreamAssessmentGroup(new UpstreamAssessmentGroup {
            this.academicYear = studentAcademicYear
            this.occurrence = studentOccurrence
            this.moduleCode = studentSitsModuleCode
            this.sequence = studentAssessmentSequence
            this.assessmentGroup = studentAssessmentGroupCode
          }, eagerLoad = true).map { uag =>
            (studentSitsModuleCode, studentAcademicYear, studentOccurrence, studentAssessmentSequence, studentAssessmentGroupCode) -> uag
          }
        }.toMap
    }

    marksToUpload.flatMap { student =>
      lazy val upstreamAssessmentGroupMember: Option[UpstreamAssessmentGroupMember] = benchmarkTask(s"Get matching UAGM - $student") {
        upstreamAssessmentGroups.get((student.moduleCode, student.academicYear, student.occurrence, student.sequence, student.assessmentGroup))
          .flatMap(_.members.asScala.find(student.matchesIdentity))
      }

      // first check to see if there is one and only one matching row
      benchmarkTask(s"Count matching SITS rows - $student") { exportFeedbackToSitsService.countMatchingSitsRecords(student) } match {
        case 0 =>
          logger.warn(s"Not updating SITS for assessment component mark $student - found zero rows")

          student.markWrittenToSitsError(RecordedAssessmentComponentStudentMarkSitsError.MissingMarksRecord)
          assessmentComponentMarksService.saveOrUpdate(student)

          None

        case r if r > 1 =>
          logger.warn(f"Not updating SITS for assessment component mark $student - found multiple rows")
          None

        case _ =>
          // update - expecting to update one row
          benchmarkTask(s"Export row to SITS - $student") { exportFeedbackToSitsService.exportToSits(student, upstreamAssessmentGroupMember) } match {
            case 0 =>
              logger.warn(s"Upload to SITS for assessment component mark $student failed - found zero rows")
              None

            case r if r > 1 =>
              throw new IllegalStateException(s"Unexpected SITS update! Only expected to update one row, but $r rows were updated for assessment component mark $student")

            case 1 =>
              student.markWrittenToSits()

              // Also update the UpstreamAssessmentGroupMember record so it doesn't show as out of sync
              benchmarkTask(s"Update UAGM - $student") {
                upstreamAssessmentGroupMember.foreach { uagm =>
                  uagm.actualMark = student.latestMark
                  uagm.actualGrade = student.latestGrade
                  uagm.agreedMark = student.latestMark.filter(_ => student.latestState.contains(MarkState.Agreed))
                  uagm.agreedGrade = student.latestGrade.filter(_ => student.latestState.contains(MarkState.Agreed))

                  assessmentMembershipService.save(uagm)
                }
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
