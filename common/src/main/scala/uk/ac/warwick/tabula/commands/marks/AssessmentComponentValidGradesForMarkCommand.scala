package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.AssessmentComponentValidGradesForMarkCommand._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, GradeBoundary}
import uk.ac.warwick.tabula.helpers.marks.{AssessmentComponentValidGradesForMarkRequest, ValidGradesForMark}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object AssessmentComponentValidGradesForMarkCommand {
  // Tuple of all valid grades and the default
  type Result = (Seq[GradeBoundary], Option[GradeBoundary])
  type Command = Appliable[Result] with SelfValidating

  val AdminPermission: Permission = Permissions.Feedback.Publish

  def apply(assessmentComponent: AssessmentComponent): Command =
    new AssessmentComponentValidGradesForMarkCommandInternal(assessmentComponent)
      with ComposableCommand[Result]
      with AssessmentComponentValidGradesForMarkPermissions
      with AssessmentComponentValidGradesForMarkValidation
      with AssessmentComponentValidGradesForMarkRequest
      with Unaudited with ReadOnly
      with AutowiringAssessmentMembershipServiceComponent
}

abstract class AssessmentComponentValidGradesForMarkCommandInternal(val assessmentComponent: AssessmentComponent)
  extends CommandInternal[Result]
    with AssessmentComponentValidGradesForMarkState {
  self: AssessmentComponentValidGradesForMarkRequest
    with AssessmentMembershipServiceComponent =>

  override def applyInternal(): (Seq[GradeBoundary], Option[GradeBoundary]) =
    ValidGradesForMark.getTuple(this, assessmentComponent)(assessmentMembershipService = assessmentMembershipService)
}

trait AssessmentComponentValidGradesForMarkState {
  def assessmentComponent: AssessmentComponent
}

trait AssessmentComponentValidGradesForMarkValidation extends SelfValidating {
  self: AssessmentComponentValidGradesForMarkRequest =>

  override def validate(errors: Errors): Unit = {
    // Prefer to just filter out invalid input in applyInternal() in this case so we still get the default
  }
}

trait AssessmentComponentValidGradesForMarkPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AssessmentComponentValidGradesForMarkState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(AdminPermission, mandatory(assessmentComponent.module))
}
