package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands.mitcircs.ListMitCircSubmissionNotesCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesNote, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ListMitCircSubmissionNotesCommand {
  type Result = Seq[MitigatingCircumstancesNote]
  type Command = Appliable[Result] with ListMitCircSubmissionNotesState
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Manage

  def apply(submission: MitigatingCircumstancesSubmission): Command =
    new ListMitCircSubmissionNotesCommandInternal(submission)
      with ComposableCommand[Result]
      with ListMitCircSubmissionNotesPermissions
      with Unaudited with ReadOnly
      with AutowiringMitCircsSubmissionServiceComponent
}

abstract class ListMitCircSubmissionNotesCommandInternal(val submission: MitigatingCircumstancesSubmission)
  extends CommandInternal[Result]
    with ListMitCircSubmissionNotesState {
  self: MitCircsSubmissionServiceComponent =>

  override def applyInternal(): Result = mitCircsSubmissionService.notesForSubmission(submission)

}

trait ListMitCircSubmissionNotesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ListMitCircSubmissionNotesState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(RequiredPermission, mandatory(submission))
  }
}

trait ListMitCircSubmissionNotesState {
  def submission: MitigatingCircumstancesSubmission
}
