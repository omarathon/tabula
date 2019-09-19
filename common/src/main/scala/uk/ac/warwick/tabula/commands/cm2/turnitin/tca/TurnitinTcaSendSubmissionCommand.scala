package uk.ac.warwick.tabula.commands.cm2.turnitin.tca

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.turnitintca.{AutowiringTurnitinTcaServiceComponent, TcaSubmission, TurnitinTcaServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.concurrent.Future

object TurnitinTcaSendSubmissionCommand {
  type CommandType = Appliable[Future[Option[TcaSubmission]]]

  def apply(attachment: FileAttachment, user: User) =
    new TurnitinTcaSendSubmissionCommandInternal(attachment, user)
    with ComposableCommand[Future[Option[TcaSubmission]]]
    with TurnitinTcaSendSubmissionCommandPermissions
    with TurnitinTcaSendSubmissionState
    with TurnitinTcaSendSubmissionDescription
    with AutowiringTurnitinTcaServiceComponent
}

class TurnitinTcaSendSubmissionCommandInternal(val attachment: FileAttachment, val user: User)
extends CommandInternal[Future[Option[TcaSubmission]]] {
  self: TurnitinTcaServiceComponent =>
  override def applyInternal(): Future[Option[TcaSubmission]] = {
    turnitinTcaService.createSubmission(attachment, user)
  }
}

trait TurnitinTcaSendSubmissionState {
  def attachment: FileAttachment
  def user: User
}

trait TurnitinTcaSendSubmissionDescription extends Describable[Future[Option[TcaSubmission]]] {
  self: TurnitinTcaSendSubmissionState =>
  override lazy val eventName = "TurnitinTCASendSubmission"

  override def describe(d: Description): Unit = {
    d.fileAttachments(Seq(attachment))
  }
}

trait TurnitinTcaSendSubmissionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: TurnitinTcaSendSubmissionState =>

  override def permissionsCheck(p: PermissionsChecking) {
    mandatory(attachment)
    p.PermissionCheck(Permissions.Submission.ViewPlagiarismStatus, attachment.submissionValue.submission)
  }
}