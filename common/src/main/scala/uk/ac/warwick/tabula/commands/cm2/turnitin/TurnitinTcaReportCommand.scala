package uk.ac.warwick.tabula.commands.cm2.turnitin

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import TurnitinTcaReportCommand._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.services.turnitintca.{AutowiringTurnitinTcaServiceComponent, TurnitinTcaServiceComponent}
import uk.ac.warwick.util.web.Uri

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object TurnitinTcaReportCommand {

  type Result = Either[String, Uri]
  type Command = Appliable[Result] with TurnitinTcaReportState with TurnitinTcaReportState
  val RequiredPermission: Permission = Permissions.Submission.ViewPlagiarismStatus

  def apply(assignment: Assignment, attachment: FileAttachment, user: CurrentUser) = new TurnitinTcaReportCommandInternal(assignment, attachment, user)
    with ComposableCommand[Result]
    with TurnitinTcaReportState
    with TurnitinTcaReportPermissions
    with TurnitinTcaReportDescription
    with AutowiringTurnitinTcaServiceComponent
    with ReadOnly
}

class TurnitinTcaReportCommandInternal(val assignment: Assignment, val attachment: FileAttachment, val user: CurrentUser) extends CommandInternal[Result]
  with TurnitinTcaReportState {

  self: TurnitinTcaReportState with TurnitinTcaServiceComponent =>

  def applyInternal(): Result = {
    Await.result(turnitinTcaService.similarityReportUrl(attachment.originalityReport, user), Duration.Inf)
  }
}

trait TurnitinTcaReportPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: TurnitinTcaReportState =>

  def permissionsCheck(p: PermissionsChecking) {
    mandatory(attachment)
    mandatory(attachment.originalityReport)
    p.PermissionCheck(Permissions.Submission.ViewPlagiarismStatus, mandatory(assignment))
  }
}

trait TurnitinTcaReportDescription extends Describable[Result] {
  self: TurnitinTcaReportState =>

  def describe(d: Description) {
    d.fileAttachments(Seq(attachment))
     .assignment(assignment)
  }
}

trait TurnitinTcaReportState {
  val assignment: Assignment
  val attachment: FileAttachment
}