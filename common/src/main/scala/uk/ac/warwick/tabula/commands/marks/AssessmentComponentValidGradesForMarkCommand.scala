package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.AssessmentComponentValidGradesForMarkCommand._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, GradeBoundary}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.util.Try

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
    with AssessmentComponentValidGradesForMarkState with Logging {
  self: AssessmentComponentValidGradesForMarkRequest
    with AssessmentMembershipServiceComponent =>

  override def applyInternal(): (Seq[GradeBoundary], Option[GradeBoundary]) = {
    val validGrades = mark.maybeText match {
      case Some(m) =>
        Try(m.toInt).toOption
          .map(asInt => assessmentMembershipService.gradesForMark(assessmentComponent, Some(asInt), Option(resitAttempt)))
          .getOrElse(Seq.empty)

      case None => assessmentMembershipService.gradesForMark(assessmentComponent, None, Option(resitAttempt))
    }

    logger.info(s"mark=$mark validGrades=$validGrades")

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

trait AssessmentComponentValidGradesForMarkState {
  def assessmentComponent: AssessmentComponent
}

trait AssessmentComponentValidGradesForMarkRequest {
  var mark: String = _
  var existing: String = _
  var resitAttempt: JInteger = _
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
