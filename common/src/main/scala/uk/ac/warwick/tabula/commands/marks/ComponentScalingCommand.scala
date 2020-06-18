package uk.ac.warwick.tabula.commands.marks

import javax.validation.constraints.{Max, Min, NotNull}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.marks.ComponentScalingCommand.Result
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand, SelfValidating}
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringModuleRegistrationServiceComponent, ProgressionService}

object ComponentScalingCommand {
  type Result = RecordAssessmentComponentMarksCommand.Result
  type Command = Appliable[Result] with SelfValidating with ComponentScalingRequest with ComponentScalingAlgorithm with MissingMarkAdjustmentStudentsToSet

  def apply(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser): Command =
    new ComponentScalingCommandInternal(assessmentComponent, upstreamAssessmentGroup, currentUser)
      with ComposableCommand[Result]
      with ComponentScalingRequest
      with ComponentScalingAlgorithm
      with ComponentScalingValidation
      with ComponentScalingDescription
      with MissingMarkAdjustmentStudentsToSet
      with RecordAssessmentComponentMarksPermissions
      with ClearRecordedModuleMarks
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringTransactionalComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringModuleRegistrationServiceComponent
}

abstract class ComponentScalingCommandInternal(val assessmentComponent: AssessmentComponent, val upstreamAssessmentGroup: UpstreamAssessmentGroup, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with RecordAssessmentComponentMarksState
    with ClearRecordedModuleMarksState {
  self: AssessmentComponentMarksServiceComponent
    with ComponentScalingRequest
    with ComponentScalingAlgorithm
    with MissingMarkAdjustmentStudentsToSet
    with TransactionalComponent
    with ClearRecordedModuleMarks =>

  // Set the default pass mark depending on the module
  passMark = assessmentComponent.module.degreeType match {
    case DegreeType.Undergraduate => ProgressionService.UndergradPassMark
    case DegreeType.Postgraduate => ProgressionService.PostgraduatePassMark
    case _ => ProgressionService.DefaultPassMark
  }
  scaledPassMark = passMark

  override def applyInternal(): Result = transactional() {
    studentsToSet.map { case (upstreamAssessmentGroupMember, mark, grade) =>
      val recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent =
        assessmentComponentMarksService.getOrCreateRecordedStudent(upstreamAssessmentGroupMember)

      val (scaledMark, scaledGrade) = scale(mark, grade, upstreamAssessmentGroupMember.isReassessment)

      recordedAssessmentComponentStudent.addMark(
        uploader = currentUser.apparentUser,
        mark = scaledMark,
        grade = scaledGrade,
        source = RecordedAssessmentComponentStudentMarkSource.Scaling,
        markState = recordedAssessmentComponentStudent.latestState.getOrElse(UnconfirmedActual),
        comments = comment(mark),
      )

      assessmentComponentMarksService.saveOrUpdate(recordedAssessmentComponentStudent)

      clearRecordedModuleMarksFor(recordedAssessmentComponentStudent)

      recordedAssessmentComponentStudent
    }
  }
}

trait ComponentScalingRequest {
  var calculate: Boolean = false

  @NotNull
  @Min(40)
  @Max(50)
  var passMark: Int = ProgressionService.DefaultPassMark

  // These Min/Max bounds are further validated below
  @NotNull
  @Min(0)
  @Max(100)
  var scaledPassMark: Int = passMark

  def passMarkAdjustment: Int = passMark - scaledPassMark

  // Can't be modified
  val upperClassMark: Int = 70

  // These Min/Max bounds are further validated below
  @NotNull
  @Min(0)
  @Max(100)
  var scaledUpperClassMark: Int = upperClassMark

  def upperClassAdjustment: Int = upperClassMark - scaledUpperClassMark

  def comment(originalMark: Option[Int]): String = s"Assessment component scaled from original mark ${originalMark.getOrElse("-")} (pass mark: $passMark, pass mark adjustment: ${if (passMarkAdjustment > 0) "+" else ""}$passMarkAdjustment, upper class adjustment: ${if (upperClassAdjustment > 0) "+" else ""}$upperClassAdjustment)"
}

// Dave's Amazing Scaling algorithm
trait ComponentScalingAlgorithm {
  self: ComponentScalingRequest
    with RecordAssessmentComponentMarksState
    with AssessmentMembershipServiceComponent =>

  def shouldScale(mark: Option[Int], grade: Option[String]): Boolean = (mark, grade) match {
    case (None, _) => false
    case (Some(0), Some(GradeBoundary.WithdrawnGrade)) => false
    case _ => true
  }

  def scale(mark: Option[Int], grade: Option[String], isResit: Boolean): (Option[Int], Option[String]) =
    if (shouldScale(mark, grade)) {
      val scaledMark = mark.map(scaleMark)

      val isIndicatorGrade = grade.exists { g =>
        assessmentMembershipService.gradesForMark(assessmentComponent, mark, isResit)
          .find(_.grade == g)
          .exists(!_.isDefault)
      }

      val scaledGrade =
        if (isIndicatorGrade) grade // Don't change indicator grades
        else assessmentMembershipService.gradesForMark(assessmentComponent, scaledMark, isResit)
          .find(_.isDefault)
          .map(_.grade)
          .orElse(grade) // Use the old grade if necessary (it shouldn't be)

      (scaledMark, scaledGrade)
    } else (mark, grade)

  def scaleMark(mark: Int): Int = {
    require(mark >= 0 && mark <= 100)

    val passMarkRange = upperClassMark - passMark

    val scaledMark: BigDecimal =
      if (mark <= passMark - passMarkAdjustment) {
        BigDecimal(mark * passMark) / (passMark - passMarkAdjustment)
      } else if (mark >= upperClassMark - upperClassAdjustment) {
        BigDecimal((mark * (100 - upperClassMark)) + 100 * upperClassAdjustment) / ((100 - upperClassMark) + upperClassAdjustment)
      } else {
        passMark + passMarkRange * (BigDecimal(passMarkAdjustment + mark - passMark) / (passMarkRange - upperClassAdjustment + passMarkAdjustment))
      }

    scaledMark.setScale(0, BigDecimal.RoundingMode.HALF_UP).toInt
  }
}

trait ComponentScalingValidation extends SelfValidating {
  self: ComponentScalingRequest
    with MissingMarkAdjustmentStudentsToSet
    with ComponentScalingAlgorithm =>

  override def validate(errors: Errors): Unit = {
    // Everyone must have an existing mark
    val studentsWithMissingMarks = studentsToSet.filter { case (_, latestMark, _) => latestMark.isEmpty }
    val hasChanges = studentsToSet.exists { case (_, mark, grade) => shouldScale(mark, grade) }

    if (studentsWithMissingMarks.nonEmpty) {
      errors.reject("scaling.studentsWithMissingMarks", Array(studentsWithMissingMarks.map(_._1.universityId).mkString(", ")), "")
    } else if (!hasChanges) {
      errors.reject("scaling.noChanges")
    }

    if (calculate) {
      if (passMarkAdjustment == 0 && upperClassAdjustment == 0) {
        errors.rejectValue("scaledUpperClassMark", "scaling.noAdjustments")
      }

      if (scaledUpperClassMark <= scaledPassMark) {
        errors.rejectValue("scaledUpperClassMark", "scaling.invalidParam")
      }

      if (passMarkAdjustment >= passMark) {
        errors.rejectValue("scaledPassMark", "scaling.invalidParam")
      } else if (passMarkAdjustment <= passMark - 100) {
        errors.rejectValue("scaledPassMark", "scaling.invalidParam")
      }

      val upperClassThreshold = 70

      if (upperClassAdjustment <= upperClassThreshold - 100) {
        errors.rejectValue("scaledUpperClassMark", "scaling.invalidParam")
      }

      if (Math.abs(upperClassAdjustment - passMarkAdjustment) >= upperClassThreshold) {
        errors.rejectValue("scaledUpperClassMark", "scaling.invalidParam")
      }
    }
  }
}

trait ComponentScalingDescription extends RecordAssessmentComponentMarksDescription {
  self: RecordAssessmentComponentMarksState =>

  override lazy val eventName: String = "ComponentScaling"
}
