package uk.ac.warwick.tabula.commands.cm2.turnitin.tca.sysadmin

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.turnitin.tca.sysadmin.TurnitinTcaRetrySimilarityReportCommand.{RequiredPermission, Result}
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.turnitintca.{AutowiringTurnitinTcaServiceComponent, TcaSimilarityReport, TcaSimilarityStatus, TurnitinTcaServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object TurnitinTcaRetrySimilarityReportCommand {

  type Result = Either[String, TcaSimilarityReport]
  type CommandType = Appliable[Result]
  val RequiredPermission: Permission = Permissions.Submission.CheckForPlagiarism

  def apply(assignment: Assignment, attachment: FileAttachment) =
    new TurnitinTcaRetrySimilarityReportCommandInternal(assignment, attachment)
      with ComposableCommand[Result]
      with TurnitinTcaRetrySimilarityReportCommandPermissions
      with TurnitinTcaRetrySimilarityReportCommandState
      with TurnitinTcaRetrySimilarityReportDescription
      with AutowiringTurnitinTcaServiceComponent
}

class TurnitinTcaRetrySimilarityReportCommandInternal(val assignment: Assignment, val attachment: FileAttachment)
  extends CommandInternal[Result] {
  self: TurnitinTcaServiceComponent with TurnitinTcaRetrySimilarityReportCommandState =>
  override def applyInternal(): Result = {
    Await.result(
      turnitinTcaService.getSimilarityReportInfo(attachment)
        .flatMap(_.fold(
          error => Future.successful(Left(error)),
          tcaSimilarityReport => {
            if (tcaSimilarityReport.status == TcaSimilarityStatus.Complete) {
              turnitinTcaService.saveSimilarityReportScores(tcaSimilarityReport, Some(attachment))
            }
            Future.successful(Right(tcaSimilarityReport))
          }
        ))
      , Duration.Inf)
  }
}

trait TurnitinTcaRetrySimilarityReportCommandState {
  def assignment: Assignment
  def attachment: FileAttachment
}

trait TurnitinTcaRetrySimilarityReportDescription extends Describable[Result] {
  self: TurnitinTcaRetrySimilarityReportCommandState =>
  override lazy val eventName = "TurnitinTcaRetrySimilarityReport"

  override def describe(d: Description): Unit = {
    d.fileAttachments(Seq(attachment))
  }
}

trait TurnitinTcaRetrySimilarityReportCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: TurnitinTcaRetrySimilarityReportCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mandatory(attachment)
    p.PermissionCheck(RequiredPermission, assignment)
  }
}
