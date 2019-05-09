package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands.mitcircs.StudentViewMitCircsSubmissionCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object StudentViewMitCircsSubmissionCommand {
  type Result = MitigatingCircumstancesSubmission
  type Command = Appliable[Result]
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Read

  def apply(submission: MitigatingCircumstancesSubmission): Command =
    new StudentViewMitCircsSubmissionCommandInternal(submission)
      with ComposableCommand[Result]
      with StudentViewMitCircsSubmissionPermissions
      with StudentViewMitCircsSubmissionDescription
}

abstract class StudentViewMitCircsSubmissionCommandInternal(val submission: MitigatingCircumstancesSubmission)
  extends CommandInternal[Result]
    with StudentViewMitCircsSubmissionState {

  override def applyInternal(): MitigatingCircumstancesSubmission = submission
}

trait StudentViewMitCircsSubmissionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentViewMitCircsSubmissionState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission, mandatory(submission))
}

trait StudentViewMitCircsSubmissionDescription extends Describable[Result] {
  self: StudentViewMitCircsSubmissionState =>

  override lazy val eventName: String = "StudentViewMitCircsSubmission"

  override def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(submission)
}

trait StudentViewMitCircsSubmissionState {
  def submission: MitigatingCircumstancesSubmission
}
