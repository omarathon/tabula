package uk.ac.warwick.tabula.commands.mitcircs

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.AddMitCircSubmissionNoteCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesNote, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object AddMitCircSubmissionNoteCommand {
  type Result = MitigatingCircumstancesNote
  type Command = Appliable[Result] with AddMitCircsSubmissionNoteState with AddMitCircsSubmissionNoteRequest with SelfValidating with BindListener
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Manage

  def apply(submission: MitigatingCircumstancesSubmission, currentUser: User): Command =
    new AddMitCircSubmissionNoteCommandInternal(submission, currentUser)
      with ComposableCommand[Result]
      with AddMitCircsSubmissionNoteRequest
      with AddMitCircsSubmissionNoteRequestBinding
      with AddMitCircsSubmissionNoteValidation
      with AddMitCircsSubmissionNotePermissions
      with AddMitCircsSubmissionNoteDescription
      with AutowiringMitCircsSubmissionServiceComponent
}

abstract class AddMitCircSubmissionNoteCommandInternal(val submission: MitigatingCircumstancesSubmission, val currentUser: User)
  extends CommandInternal[Result]
    with AddMitCircsSubmissionNoteState {
  self: AddMitCircsSubmissionNoteRequest
    with MitCircsSubmissionServiceComponent =>

  override def applyInternal(): Result = transactional() {
    require(submission.canAddNote, "Can't add notes to this submission")

    val note = new MitigatingCircumstancesNote(submission, currentUser)
    note.text = text
    file.attached.asScala.foreach(note.addAttachment)
    mitCircsSubmissionService.create(note)
  }

}

trait AddMitCircsSubmissionNoteValidation extends SelfValidating {
  self: AddMitCircsSubmissionNoteRequest =>

  override def validate(errors: Errors): Unit = {
    if (!text.hasText)
      errors.rejectValue("text", "mitigatingCircumstances.note.text.required")
  }
}

trait AddMitCircsSubmissionNotePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AddMitCircsSubmissionNoteState =>

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(RequiredPermission, mandatory(submission))
  }
}

trait AddMitCircsSubmissionNoteDescription extends Describable[Result] {
  self: AddMitCircsSubmissionNoteState =>

  override lazy val eventName: String = "AddMitCircsSubmissionNote"

  override def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(submission)
}

trait AddMitCircsSubmissionNoteState {
  def submission: MitigatingCircumstancesSubmission
  val currentUser: User
}

trait AddMitCircsSubmissionNoteRequest {
  var text: String = _
  var file: UploadedFile = new UploadedFile
}

trait AddMitCircsSubmissionNoteRequestBinding extends BindListener {
  self: AddMitCircsSubmissionNoteRequest =>

  override def onBind(result: BindingResult): Unit = transactional() {
    file.onBind(result)
  }
}
