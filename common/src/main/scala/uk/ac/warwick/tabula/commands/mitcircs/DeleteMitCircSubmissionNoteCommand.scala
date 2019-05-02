package uk.ac.warwick.tabula.commands.mitcircs

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.DeleteMitCircSubmissionNoteCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesNote, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object DeleteMitCircSubmissionNoteCommand {
  type Result = MitigatingCircumstancesNote
  type Command = Appliable[Result] with DeleteMitCircsSubmissionNoteState with DeleteMitCircsSubmissionNoteRequest with SelfValidating
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Manage

  def apply(submission: MitigatingCircumstancesSubmission, note: MitigatingCircumstancesNote): Command =
    new DeleteMitCircSubmissionNoteCommandInternal(submission, note)
      with ComposableCommand[Result]
      with DeleteMitCircsSubmissionNoteRequest
      with DeleteMitCircsSubmissionNoteValidation
      with DeleteMitCircsSubmissionNotePermissions
      with DeleteMitCircsSubmissionNoteDescription
      with AutowiringMitCircsSubmissionServiceComponent
}

abstract class DeleteMitCircSubmissionNoteCommandInternal(val submission: MitigatingCircumstancesSubmission, val note: MitigatingCircumstancesNote)
  extends CommandInternal[Result]
    with DeleteMitCircsSubmissionNoteState {
  self: DeleteMitCircsSubmissionNoteRequest
    with MitCircsSubmissionServiceComponent =>

  override def applyInternal(): Result = transactional() {
    mitCircsSubmissionService.delete(note)
  }

}

trait DeleteMitCircsSubmissionNoteValidation extends SelfValidating {
  self: DeleteMitCircsSubmissionNoteRequest =>

  override def validate(errors: Errors): Unit = {}
}

trait DeleteMitCircsSubmissionNotePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: DeleteMitCircsSubmissionNoteState =>

  override def permissionsCheck(p: PermissionsChecking) {
    mustBeLinked(note, submission)
    p.PermissionCheck(RequiredPermission, submission)
  }
}

trait DeleteMitCircsSubmissionNoteDescription extends Describable[Result] {
  self: DeleteMitCircsSubmissionNoteState =>

  override def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(submission)
}

trait DeleteMitCircsSubmissionNoteState {
  def submission: MitigatingCircumstancesSubmission
  def note: MitigatingCircumstancesNote
}

trait DeleteMitCircsSubmissionNoteRequest {
  var text: String = _
}
