package uk.ac.warwick.tabula.commands.cm2.turnitin.tca

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.turnitintca.{AutowiringTurnitinTcaServiceComponent, TcaSubmission, TurnitinTcaServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object TurnitinTcaSubmissionUploadFileCommand {
  type CommandType = Appliable[Unit]

  def apply(attachment: FileAttachment, user: User) =
    new TurnitinTcaSubmissionUploadFileCommandInternal(attachment, user)
    with ComposableCommand[Unit]
    with TurnitinTcaSubmissionUploadFileCommandPermissions
    with TurnitinTcaSubmissionUploadFileCommandState
    with TurnitinTcaSubmissionUploadFileCommandDescription
    with AutowiringTurnitinTcaServiceComponent
}

class TurnitinTcaSubmissionUploadFileCommandInternal(val attachment: FileAttachment, val user: User)
extends CommandInternal[Unit] {
  self: TurnitinTcaServiceComponent =>
  override def applyInternal(): Unit = {
    Await.result(turnitinTcaService.uploadSubmissionFile(attachment), Duration.Inf)
  }
}

trait TurnitinTcaSubmissionUploadFileCommandState {
  def attachment: FileAttachment
  def user: User
}

trait TurnitinTcaSubmissionUploadFileCommandDescription extends Describable[Unit] {
  self: TurnitinTcaSubmissionUploadFileCommandState =>
  override lazy val eventName = "TurnitinTCASubmissionUploadFile"

  override def describe(d: Description): Unit = {
    d.fileAttachments(Seq(attachment))
  }

  override def describeResult(d: Description, result: Unit): Unit = {
    d.property("uploaded" -> "true")
  }
}

trait TurnitinTcaSubmissionUploadFileCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: TurnitinTcaSubmissionUploadFileCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    mandatory(attachment)
    p.PermissionCheck(Permissions.Submission.CheckForPlagiarism, attachment.submissionValue.submission)
  }
}