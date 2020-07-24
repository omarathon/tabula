package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ModuleRegistrationValidGradesForMarkCommand._
import uk.ac.warwick.tabula.data.model.{GradeBoundary, ModuleRegistration}
import uk.ac.warwick.tabula.helpers.marks.{ModuleRegistrationValidGradesForMarkRequest, ValidGradesForMark}
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

  override def applyInternal(): (Seq[GradeBoundary], Option[GradeBoundary]) =
    ValidGradesForMark.getTuple(this, moduleRegistration)(assessmentMembershipService = assessmentMembershipService)
}

trait ModuleRegistrationValidGradesForMarkState {
  def moduleRegistration: ModuleRegistration
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
