package uk.ac.warwick.tabula.commands.cm2.turnitin.tca

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.turnitin.tca.TurnitinTcaSendSubmissionCommand.Result
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.turnitintca.{AutowiringTurnitinTcaServiceComponent, TcaSubmission, TurnitinTcaServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object TurnitinTcaSendSubmissionCommand {

  type Result = Either[String, TcaSubmission]
  type CommandType = Appliable[Result]

  def apply(attachment: FileAttachment, user: User) =
    new TurnitinTcaSendSubmissionCommandInternal(attachment, user)
    with ComposableCommand[Result]
    with TurnitinTcaSendSubmissionCommandPermissions
    with TurnitinTcaSendSubmissionState
    with TurnitinTcaSendSubmissionDescription
    with AutowiringTurnitinTcaServiceComponent
}

class TurnitinTcaSendSubmissionCommandInternal(val attachment: FileAttachment, val user: User)
extends CommandInternal[Result] {
  self: TurnitinTcaServiceComponent =>
  override def applyInternal(): Result = {
    Await.result(turnitinTcaService.createSubmission(attachment, user), Duration.Inf)
  }
}

trait TurnitinTcaSendSubmissionState {
  def attachment: FileAttachment
  def user: User
}

trait TurnitinTcaSendSubmissionDescription extends Describable[Result] {
  self: TurnitinTcaSendSubmissionState =>
  override lazy val eventName = "TurnitinTCASendSubmission"

  override def describe(d: Description): Unit = {
    d.fileAttachments(Seq(attachment))
  }

  override def describeResult(d: Description, result: Result): Unit =
    result.fold(
      error => d.properties(
        "tcaSubmissionCreated" -> false,
        "tcaError" -> error
      ),
      tcaSubmission => d.properties(
        "tcaSubmissionCreated" -> true,
        "tcaSubmissionId" -> tcaSubmission.id,
        "tcaStatus" -> tcaSubmission.status,
        "tcaCreatedTime" -> tcaSubmission.created.toString
      )
    )
}

trait TurnitinTcaSendSubmissionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: TurnitinTcaSendSubmissionState =>

  override def permissionsCheck(p: PermissionsChecking) {
    mandatory(attachment)
    p.PermissionCheck(Permissions.Submission.ViewPlagiarismStatus, attachment.submissionValue.submission)
  }
}
