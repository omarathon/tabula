package uk.ac.warwick.tabula.commands.cm2.turnitin.tca

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.turnitin.tca.TurnitinTcaSendSubmissionCommand.{Result, _}
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.turnitintca.{AutowiringTurnitinTcaServiceComponent, TcaSubmission, TurnitinTcaServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object TurnitinTcaSendSubmissionCommand {

  type Result = Map[FileAttachment, Either[String, TcaSubmission]]
  type CommandType = Appliable[Result]
  val RequiredPermission: Permission = Permissions.Submission.CheckForPlagiarism

  def apply(assignment: Assignment, user: User) =
    new TurnitinTcaSendSubmissionCommandInternal(assignment, user)
    with ComposableCommand[Result]
    with TurnitinTcaSendSubmissionCommandPermissions
    with TurnitinTcaSendSubmissionState
    with TurnitinTcaSendSubmissionDescription
    with AutowiringTurnitinTcaServiceComponent
}

class TurnitinTcaSendSubmissionCommandInternal(val assignment: Assignment, val user: User)
extends CommandInternal[Result] {
  self: TurnitinTcaServiceComponent with TurnitinTcaSendSubmissionState  =>
  override def applyInternal(): Result = {
    Await.result(Future.sequence(
      attachments
        .map(a => turnitinTcaService.createSubmission(a, user)
          .flatMap(_.fold(
            error => Future.successful(Left(error)),
            tcaSubmission => turnitinTcaService.uploadSubmissionFile(a, tcaSubmission)
          ))
          .recover{ case e => Left(e.getMessage) }
          .map(t => a -> t)
        )
    ), Duration.Inf).toMap
  }
}

trait TurnitinTcaSendSubmissionState {
  def assignment: Assignment
  def user: User
  def attachments: Seq[FileAttachment] = assignment.submissions.asScala.toSeq.flatMap(_.allAttachments).filter(_.originalityReport == null)
}

trait TurnitinTcaSendSubmissionDescription extends Describable[Result] {
  self: TurnitinTcaSendSubmissionState =>
  override lazy val eventName = "TurnitinTCASendSubmission"

  override def describe(d: Description): Unit = {
    d.assignment(assignment)
     .fileAttachments(attachments)
  }

  override def describeResult(d: Description, result: Result): Unit = {
    val submissions = result.filter{ case (_, e) => e.isRight }.map{ case (fa, e) => fa -> e.right.get }
    val errors = result.filter{ case (_, e) => e.isLeft }.map{ case (fa, e) => fa -> e.left.get }

    d.properties("tcaSubmissionsCreated" -> submissions.map{ case (fa, s) => s"${fa.id} - ${s.id}" })
    d.properties("tcaErrors" -> errors.map{ case (fa, e) => s"${fa.id} - $e" })
  }
}

trait TurnitinTcaSendSubmissionCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: TurnitinTcaSendSubmissionState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mandatory(assignment)
    p.PermissionCheck(RequiredPermission, assignment)
  }
}
