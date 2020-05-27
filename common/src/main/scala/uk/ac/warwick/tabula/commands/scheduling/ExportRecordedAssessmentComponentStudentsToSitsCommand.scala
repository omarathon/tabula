package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.scheduling.ExportRecordedAssessmentComponentStudentsToSitsCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.data.model.{RecordedAssessmentComponentStudent, UpstreamAssessmentGroup, UpstreamAssessmentGroupMember}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.{AutowiringExportFeedbackToSitsServiceComponent, ExportFeedbackToSitsServiceComponent}

import scala.jdk.CollectionConverters._

object ExportRecordedAssessmentComponentStudentsToSitsCommand {
  type Result = Seq[RecordedAssessmentComponentStudent]
  type Command = Appliable[Result]

  def apply(): Command =
    new ExportRecordedAssessmentComponentStudentsToSitsCommandInternal()
      with ComposableCommand[Result]
      with ExportFeedbackToSitsCommandPermissions
      with ExportRecordedAssessmentComponentStudentsToSitsDescription
      with AutowiringExportFeedbackToSitsServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringTransactionalComponent
}

abstract class ExportRecordedAssessmentComponentStudentsToSitsCommandInternal
  extends CommandInternal[Result]
    with Logging {
  self: ExportFeedbackToSitsServiceComponent
    with AssessmentComponentMarksServiceComponent
    with AssessmentMembershipServiceComponent
    with ModuleAndDepartmentServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional() {
    val marksToUpload = assessmentComponentMarksService.allNeedingWritingToSits

    marksToUpload.flatMap { student =>
      val canUploadMarksToSitsForYear =
        moduleAndDepartmentService.getModuleBySitsCode(student.moduleCode).forall { module =>
          module.adminDepartment.canUploadMarksToSitsForYear(student.academicYear, module)
        }

      val upstreamAssessmentGroupMember: Option[UpstreamAssessmentGroupMember] =
        assessmentMembershipService.getUpstreamAssessmentGroup(new UpstreamAssessmentGroup {
          this.academicYear = student.academicYear
          this.occurrence = student.occurrence
          this.moduleCode = student.moduleCode
          this.sequence = student.sequence
          this.assessmentGroup = student.assessmentGroup
        }).flatMap(_.members.asScala.find(_.universityId == student.universityId))

      val resit: Boolean = upstreamAssessmentGroupMember.exists(_.resitExpected.contains(true))

      if (!canUploadMarksToSitsForYear) {
        logger.warn(s"Not uploading assessment component mark $student as department for ${student.moduleCode} is closed for ${student.academicYear}")
        None
      } else {
        // first check to see if there is one and only one matching row
        exportFeedbackToSitsService.countMatchingSitsRecords(student, resit) match {
          case 0 =>
            logger.warn(s"Not updating SITS for assessment component mark $student - found zero rows")
            None

          case r if r > 1 =>
            logger.warn(f"Not updating SITS for assessment component mark $student - found multiple rows")
            None

          case _ =>
            // update - expecting to update one row
            exportFeedbackToSitsService.exportToSits(student, resit) match {
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
                  if (resit) {
                    uagm.resitActualMark = student.latestMark
                    uagm.resitActualGrade = student.latestGrade
                    uagm.resitAgreedMark = None
                    uagm.resitAgreedGrade = None
                  } else {
                    uagm.actualMark = student.latestMark
                    uagm.actualGrade = student.latestGrade
                    uagm.agreedMark = None
                    uagm.agreedGrade = None
                  }

                  assessmentMembershipService.save(uagm)
                }

                Some(assessmentComponentMarksService.saveOrUpdate(student))
            }
        }
      }
    }
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
