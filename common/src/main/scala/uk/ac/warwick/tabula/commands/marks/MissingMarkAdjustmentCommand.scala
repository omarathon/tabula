package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.marks.MissingMarkAdjustmentCommand.Result
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand, SelfValidating}
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}

import scala.jdk.CollectionConverters._

object MissingMarkAdjustmentCommand {
  type Result = RecordAssessmentComponentMarksCommand.Result
  type Command = Appliable[Result] with SelfValidating with MissingMarkAdjustmentStudentsToSet

  def apply(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser): Command =
    new MissingMarkAdjustmentCommandInternal(assessmentComponent, upstreamAssessmentGroup, currentUser)
      with ComposableCommand[Result]
      with MissingMarkAdjustmentValidation
      with MissingMarkAdjustmentDescription
      with MissingMarkAdjustmentStudentsToSet
      with RecordAssessmentComponentMarksPermissions
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringTransactionalComponent
}

abstract class MissingMarkAdjustmentCommandInternal(val assessmentComponent: AssessmentComponent, val upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser)
  extends CommandInternal[Result]
    with RecordAssessmentComponentMarksState {
  self: AssessmentComponentMarksServiceComponent
    with MissingMarkAdjustmentStudentsToSet
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional() {
    studentsToSet.map { case (upstreamAssessmentGroupMember, _, _) =>
      val recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent =
        assessmentComponentMarksService.getOrCreateRecordedStudent(upstreamAssessmentGroupMember)

      recordedAssessmentComponentStudent.addMark(
        uploader = currentUser.apparentUser,
        mark = None,
        grade = Some(GradeBoundary.ForceMajeureMissingComponentGrade),
        source = RecordedAssessmentComponentStudentMarkSource.MissingMarkAdjustment,
        markState = recordedAssessmentComponentStudent.latestState.getOrElse(UnconfirmedActual),
        comments = "Assessment did not take place because of force majeure",
      )

      assessmentComponentMarksService.saveOrUpdate(recordedAssessmentComponentStudent)

      recordedAssessmentComponentStudent
    }
  }
}

trait MissingMarkAdjustmentStudentsToSet {
  self: RecordAssessmentComponentMarksState
    with AssessmentComponentMarksServiceComponent =>

  // All students in this existing group and their latest mark and grade
  lazy val allStudents: Seq[(UpstreamAssessmentGroupMember, Option[Int], Option[String])] = {
    val allRecordedStudents = assessmentComponentMarksService.getAllRecordedStudents(upstreamAssessmentGroup)

    upstreamAssessmentGroup.members.asScala.map { upstreamAssessmentGroupMember =>
      val latestMark =
        allRecordedStudents.find(_.universityId == upstreamAssessmentGroupMember.universityId)
          .flatMap(_.latestMark)
          .orElse(upstreamAssessmentGroupMember.firstDefinedMark)

      val latestGrade =
        allRecordedStudents.find(_.universityId == upstreamAssessmentGroupMember.universityId)
          .flatMap(_.latestGrade)
          .orElse(upstreamAssessmentGroupMember.firstDefinedGrade)

      (upstreamAssessmentGroupMember, latestMark, latestGrade)
    }.toSeq.sortBy(_._1.universityId)
  }

  // We don't let this happen if there are any existing student marks other 0/W or if it's a no-op
  lazy val studentsToSet: Seq[(UpstreamAssessmentGroupMember, Option[Int], Option[String])] =
    allStudents.filterNot { case (_, latestMark, latestGrade) =>
      (latestMark.contains(0) && latestGrade.contains(GradeBoundary.WithdrawnGrade)) ||
      (latestMark.isEmpty && latestGrade.contains(GradeBoundary.ForceMajeureMissingComponentGrade))
    }

}

trait MissingMarkAdjustmentValidation extends SelfValidating {
  self: MissingMarkAdjustmentStudentsToSet =>

  override def validate(errors: Errors): Unit = {
    val studentsWithExistingMarks = allStudents.filter { case (_, latestMark, latestGrade) =>
      latestMark.exists(_ > 0) || latestGrade.exists(g => g != GradeBoundary.WithdrawnGrade && g != GradeBoundary.ForceMajeureMissingComponentGrade)
    }

    val hasChanges = studentsToSet.nonEmpty

    if (studentsWithExistingMarks.nonEmpty) {
      errors.reject("missingMarks.studentsWithMarks", Array(studentsWithExistingMarks.map(_._1.universityId).mkString(", ")), "")
    } else if (!hasChanges) {
      errors.reject("missingMarks.noChanges")
    }
  }
}

trait MissingMarkAdjustmentDescription extends RecordAssessmentComponentMarksDescription {
  self: RecordAssessmentComponentMarksState =>

  override lazy val eventName: String = "MissingMarkAdjustment"
}
