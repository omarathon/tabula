package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ValidGradesForMarkCommand._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, GradeBoundary}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.util.Try

object ValidGradesForMarkCommand {
  // Tuple of all valid grades and the default
  type Result = (Seq[GradeBoundary], Option[GradeBoundary])
  type Command = Appliable[Result] with SelfValidating

  val AdminPermission: Permission = Permissions.Feedback.Publish

  def apply(assessmentComponent: AssessmentComponent): Command =
    new ValidGradesForMarkCommandInternal(assessmentComponent)
      with ComposableCommand[Result]
      with ValidGradesForMarkPermissions
      with ValidGradesForMarkValidation
      with ValidGradesForMarkRequest
      with Unaudited with ReadOnly
      with AutowiringAssessmentMembershipServiceComponent
}

abstract class ValidGradesForMarkCommandInternal(val assessmentComponent: AssessmentComponent)
  extends CommandInternal[Result]
    with ValidGradesForMarkState {
  self: ValidGradesForMarkRequest
    with AssessmentMembershipServiceComponent =>

  override def applyInternal(): (Seq[GradeBoundary], Option[GradeBoundary]) = {
    val validGrades = mark.maybeText match {
      case Some(m) =>
        Try(m.toInt).toOption
          .map(asInt => assessmentMembershipService.gradesForMark(assessmentComponent, Some(asInt)))
          .getOrElse(Seq.empty)

      case None => assessmentMembershipService.gradesForMark(assessmentComponent, None)
    }

    val default =
      if (existing.maybeText.nonEmpty && validGrades.exists(_.grade == existing)) {
        validGrades.find(_.grade == existing)
      } else {
        if (!assessmentComponent.module.adminDepartment.assignmentGradeValidationUseDefaultForZero && mark == "0") {
          None // TAB-3499
        } else {
          validGrades.find(_.isDefault)
        }
      }

    (validGrades, default)
  }
}

trait ValidGradesForMarkState {
  def assessmentComponent: AssessmentComponent
}

trait ValidGradesForMarkRequest {
  var mark: String = _
  var existing: String = _
}

trait ValidGradesForMarkValidation extends SelfValidating {
  self: ValidGradesForMarkRequest =>

  override def validate(errors: Errors): Unit = {
    // Prefer to just filter out invalid input in applyInternal() in this case so we still get the default
  }
}

trait ValidGradesForMarkPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ValidGradesForMarkState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(AdminPermission, mandatory(assessmentComponent.module))
}
