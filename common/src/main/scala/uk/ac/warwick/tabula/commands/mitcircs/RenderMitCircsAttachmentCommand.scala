package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.RenderMitCircsAttachmentCommand._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object RenderMitCircsAttachmentCommand {
  type Result = Option[RenderableAttachment]
  type Command = Appliable[Result] with RenderMitCircsAttachmentState
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Read

  def apply(submission: MitigatingCircumstancesSubmission, filename: String): Command =
    new RenderMitCircsAttachmentCommandInternal(submission, filename)
      with ComposableCommand[Result]
      with RenderMitCircsAttachmentPermissions
      with RenderMitCircsAttachmentDescription
}

abstract class RenderMitCircsAttachmentCommandInternal(val submission: MitigatingCircumstancesSubmission, val filename: String)
  extends CommandInternal[Result]
    with RenderMitCircsAttachmentState {

  override def applyInternal(): Option[RenderableAttachment] =
    submission.attachments
      .filter(_.hasData)
      .find(_.name == filename)
      .map(a => new RenderableAttachment(a))

}

trait RenderMitCircsAttachmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RenderMitCircsAttachmentState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission, mandatory(submission))
}

trait RenderMitCircsAttachmentDescription extends Describable[Result] {
  self: RenderMitCircsAttachmentState =>

  override lazy val eventName: String = "RenderMitCircsAttachment"

  override def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(submission)
     .property("filename", filename)

  override def describeResult(d: Description, result: Option[RenderableAttachment]): Unit =
    d.property("fileFound", result.nonEmpty)
}

trait RenderMitCircsAttachmentState {
  val submission: MitigatingCircumstancesSubmission
  val filename: String
}
