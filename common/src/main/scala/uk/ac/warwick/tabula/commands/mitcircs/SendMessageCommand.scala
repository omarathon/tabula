package uk.ac.warwick.tabula.commands.mitcircs

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.{MitCircsMessageFromStudentNotification, MitCircsMessageToStudentNotification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConverters._

object SendMessageCommand {
  def apply(submission: MitigatingCircumstancesSubmission, currentUser: User) = new SendMessageCommandInternal(submission, currentUser)
    with ComposableCommand[MitigatingCircumstancesMessage]
    with SendMessageValidation
    with SendMessagePermissions
    with SendMessageDescription
    with SendMessageNotifications
    with AutowiringMitCircsSubmissionServiceComponent
}

class SendMessageCommandInternal(val submission: MitigatingCircumstancesSubmission, val currentUser: User) extends CommandInternal[MitigatingCircumstancesMessage]
  with SendMessageState with BindListener  {

  self: MitCircsSubmissionServiceComponent =>

  override def onBind(result: BindingResult): Unit = transactional() {
    file.onBind(result)
  }

  def applyInternal(): MitigatingCircumstancesMessage = {
    val newMessage = new MitigatingCircumstancesMessage(submission, currentUser)
    newMessage.message = message
    file.attached.asScala.foreach(newMessage.addAttachment)
    mitCircsSubmissionService.create(newMessage)
  }
}

trait SendMessageValidation extends SelfValidating {
  self: SendMessageState =>

  override def validate(errors: Errors) {
    if(!message.hasText) errors.rejectValue("message", "mitigatingCircumstances.message.required")
  }
}

trait SendMessagePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: SendMessageState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.MitigatingCircumstancesSubmission.Modify, submission)
  }
}

trait SendMessageDescription extends Describable[MitigatingCircumstancesMessage] {
  self: SendMessageState =>

  override lazy val eventName: String = "SendMessage"

  def describe(d: Description) {
    d.mitigatingCircumstancesSubmission(submission)
  }
}

trait SendMessageState {
  val submission: MitigatingCircumstancesSubmission
  val currentUser: User
  var message: String = _
  var file: UploadedFile = new UploadedFile
}

trait SendMessageNotifications extends Notifies[MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission] {

  self: SendMessageState =>

  def emit(message: MitigatingCircumstancesMessage): Seq[Notification[MitigatingCircumstancesMessage, MitigatingCircumstancesSubmission]] = {
    if(currentUser == submission.student.asSsoUser) Seq(Notification.init(new MitCircsMessageFromStudentNotification, currentUser, message, submission))
    else Seq(Notification.init(new MitCircsMessageToStudentNotification, currentUser, message, submission))
  }
}