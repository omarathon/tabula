package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ModuleRegistrationValidGradesForMarkCommand._
import uk.ac.warwick.tabula.data.model.{GradeBoundary, ModuleRegistration}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.util.Try

object ModuleRegistrationValidGradesForMarkCommand {
  // Tuple of all valid grades and the default
  type Result = (Seq[GradeBoundary], Option[GradeBoundary])
  type Command = Appliable[Result] with SelfValidating

  val AdminPermission: Permission = Permissions.Feedback.Publish

  def apply(moduleRegistration: ModuleRegistration): Command =
    new ModuleRegistrationValidGradesForMarkCommandInternal(moduleRegistration)
      with ComposableCommand[Result]
      with ModuleRegistrationValidGradesForMarkPermissions
      with ModuleRegistrationValidGradesForMarkValidation
      with ModuleRegistrationValidGradesForMarkRequest
      with Unaudited with ReadOnly
      with AutowiringAssessmentMembershipServiceComponent
}

abstract class ModuleRegistrationValidGradesForMarkCommandInternal(val moduleRegistration: ModuleRegistration)
  extends CommandInternal[Result]
    with ModuleRegistrationValidGradesForMarkState {
  self: ModuleRegistrationValidGradesForMarkRequest
    with AssessmentMembershipServiceComponent =>

  override def applyInternal(): (Seq[GradeBoundary], Option[GradeBoundary]) = {
    val validGrades = mark.maybeText match {
      case Some(m) =>
        Try(m.toInt).toOption
          .map(asInt => assessmentMembershipService.gradesForMark(moduleRegistration, Some(asInt), resit))
          .getOrElse(Seq.empty)

      case None => assessmentMembershipService.gradesForMark(moduleRegistration, None, resit)
    }

    val default =
      if (existing.maybeText.nonEmpty && validGrades.exists(_.grade == existing)) {
        validGrades.find(_.grade == existing)
      } else {
        if (!moduleRegistration.module.adminDepartment.assignmentGradeValidationUseDefaultForZero && mark == "0") {
          None // TAB-3499
        } else {
          validGrades.find(_.isDefault)
        }
      }

    (validGrades, default)
  }
}

trait ModuleRegistrationValidGradesForMarkState {
  def moduleRegistration: ModuleRegistration
}

trait ModuleRegistrationValidGradesForMarkRequest {
  var mark: String = _
  var existing: String = _
  var resit: Boolean = false
}

trait ModuleRegistrationValidGradesForMarkValidation extends SelfValidating {
  self: ModuleRegistrationValidGradesForMarkRequest =>

  override def validate(errors: Errors): Unit = {
    // Prefer to just filter out invalid input in applyInternal() in this case so we still get the default
  }
}

trait ModuleRegistrationValidGradesForMarkPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ModuleRegistrationValidGradesForMarkState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(AdminPermission, mandatory(moduleRegistration))
}
