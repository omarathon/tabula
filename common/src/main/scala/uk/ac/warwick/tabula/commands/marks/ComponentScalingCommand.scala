package uk.ac.warwick.tabula.commands.marks

import javax.validation.constraints.{Max, Min, NotNull}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.marks.ComponentScalingCommand.Result
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand, SelfValidating}
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}

object ComponentScalingCommand {
  type Result = RecordAssessmentComponentMarksCommand.Result
  type Command = Appliable[Result] with SelfValidating

  def apply(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser): Command =
    new ComponentScalingCommandInternal(assessmentComponent, upstreamAssessmentGroup, currentUser)
      with ComposableCommand[Result]
      with ComponentScalingValidation
      with ComponentScalingDescription
      with RecordAssessmentComponentMarksPermissions
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringTransactionalComponent
}

abstract class ComponentScalingCommandInternal(val assessmentComponent: AssessmentComponent, val upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser)
  extends CommandInternal[Result]
    with RecordAssessmentComponentMarksState {
  self: AssessmentComponentMarksServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional() {
    ???
  }
}

trait ComponentScalingRequest {
  @NotNull
  @Min(1)
  @Max(100)
  var lowerBound: Int = 5

  @NotNull
  @Min(1)
  @Max(100)
  var upperBound: Int = 5

  @NotNull
  @Min(40)
  @Max(50)
  var passThreshold: Int = 40
}

// Dave's Amazing Scaling algorithm
trait ComponentScalingAlgorithm {
  self: ComponentScalingRequest =>

  def scale(mark: Int): Int = {
    require(mark >= 0 && mark <= 100)

    val upperClassThreshold = 70
    val passMarkRange = upperClassThreshold - passThreshold

    val scaledMark: BigDecimal =
      if (mark <= passThreshold - lowerBound) {
        BigDecimal(mark * passThreshold) / (passThreshold - lowerBound)
      } else if (mark >= upperClassThreshold - upperBound) {
        BigDecimal((mark * passMarkRange) + 100 * upperBound) / (passMarkRange + upperBound)
      } else {
        passThreshold + passMarkRange * (BigDecimal(lowerBound + mark - passThreshold) / (passMarkRange - upperBound + lowerBound))
      }

    scaledMark.setScale(0, BigDecimal.RoundingMode.HALF_UP).toInt
  }
}

trait ComponentScalingValidation extends SelfValidating {
  override def validate(errors: Errors): Unit = ???
}

trait ComponentScalingDescription extends RecordAssessmentComponentMarksDescription {
  self: RecordAssessmentComponentMarksState =>

  override lazy val eventName: String = "ComponentScaling"
}
