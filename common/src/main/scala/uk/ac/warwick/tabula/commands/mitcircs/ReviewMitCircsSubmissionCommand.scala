package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.ReviewMitCircsSubmissionCommand._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ReviewMitCircsSubmissionCommand {
  type Result = MitigatingCircumstancesSubmission
  type Command = Appliable[Result]
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Manage

  def apply(submission: MitigatingCircumstancesSubmission): Command =
    new ReviewMitCircsSubmissionCommandInternal(submission)
      with ComposableCommand[Result]
      with ReviewMitCircsSubmissionPermissions
      with ReviewMitCircsSubmissionDescription
}

abstract class ReviewMitCircsSubmissionCommandInternal(val submission: MitigatingCircumstancesSubmission)
  extends CommandInternal[Result]
    with ReviewMitCircsSubmissionState {

  override def applyInternal(): MitigatingCircumstancesSubmission = submission
}

trait ReviewMitCircsSubmissionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ReviewMitCircsSubmissionState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission, mandatory(submission))
}

trait ReviewMitCircsSubmissionDescription extends Describable[Result] {
  self: ReviewMitCircsSubmissionState =>

  override lazy val eventName: String = "ReviewMitCircsSubmission"

  override def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(submission)
}

trait ReviewMitCircsSubmissionState {
  def submission: MitigatingCircumstancesSubmission
}
