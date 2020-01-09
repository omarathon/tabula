package uk.ac.warwick.tabula.commands.cm2.turnitin.tca.sysadmin

import uk.ac.warwick.tabula.commands.cm2.turnitin.tca.sysadmin.TurnitinTcaRetrySubmissionCommand.{RequiredPermission, Result}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.turnitintca.{AutowiringTurnitinTcaServiceComponent, TcaSubmission, TcaSubmissionStatus, TurnitinTcaServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object TurnitinTcaRetrySubmissionCommand {

  type Result = Either[String, TcaSubmission]
  type CommandType = Appliable[Result]
  val RequiredPermission: Permission = Permissions.Submission.CheckForPlagiarism

  def apply(assignment: Assignment, attachment: FileAttachment, user: User) =
    new TurnitinTcaRetrySubmissionCommandInternal(assignment, attachment, user)
      with ComposableCommand[Result]
      with TurnitinTcaRetrySubmissionCommandPermissions
      with TurnitinTcaRetrySubmissionCommandState
      with TurnitinTcaRetrySubmissionDescription
      with AutowiringTurnitinTcaServiceComponent

}

class TurnitinTcaRetrySubmissionCommandInternal(val assignment: Assignment, val attachment: FileAttachment, val user: User)
  extends CommandInternal[Result] {
  self: TurnitinTcaServiceComponent with TurnitinTcaRetrySubmissionCommandState  =>
  override def applyInternal(): Result = {
    Await.result(
      turnitinTcaService.getSubmissionInfo(attachment, user)
        .flatMap(_.fold(
          error => Future.successful(Left(error)),
          tcaSubmission =>
            if (tcaSubmission.status == TcaSubmissionStatus.Created) {
              turnitinTcaService.uploadSubmissionFile(attachment, tcaSubmission)
            } else if (tcaSubmission.status == TcaSubmissionStatus.Complete) {
              turnitinTcaService.requestSimilarityReport(tcaSubmission, Some(attachment)).map(
                _ => Right(tcaSubmission)
              )
            } else Future.successful(Right(tcaSubmission))
        ))
      , Duration.Inf)
  }

}

trait TurnitinTcaRetrySubmissionCommandState {
  def assignment: Assignment
  def attachment: FileAttachment
  def user: User
}

trait TurnitinTcaRetrySubmissionDescription extends Describable[Result] {
  self: TurnitinTcaRetrySubmissionCommandState =>
  override lazy val eventName = "TurnitinTcaRetrySubmission"

  override def describe(d: Description): Unit = {
    d.fileAttachments(Seq(attachment))
  }

}

trait TurnitinTcaRetrySubmissionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: TurnitinTcaRetrySubmissionCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mandatory(attachment)
    p.PermissionCheck(RequiredPermission, assignment)
  }
}
