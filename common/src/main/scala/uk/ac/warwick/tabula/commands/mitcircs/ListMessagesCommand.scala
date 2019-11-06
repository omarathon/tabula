package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesStudent, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}


object ListMessagesCommand {
  def apply(submission: MitigatingCircumstancesSubmission) = new ListMessagesCommandInternal(submission)
    with ComposableCommand[Seq[MitigatingCircumstancesMessage]]
    with ListMessagesPermissions
    with Unaudited with ReadOnly
    with AutowiringMitCircsSubmissionServiceComponent
}

class ListMessagesCommandInternal(val submission: MitigatingCircumstancesSubmission) extends CommandInternal[Seq[MitigatingCircumstancesMessage]]
  with ListMessagesState {

  self: MitCircsSubmissionServiceComponent  =>

  def applyInternal(): Seq[MitigatingCircumstancesMessage] = mitCircsSubmissionService.messagesForSubmission(submission)
}

trait ListMessagesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ListMessagesState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.MitigatingCircumstancesSubmission.Read, submission)
  }
}

trait ListMessagesState {
  val submission: MitigatingCircumstancesSubmission
}
