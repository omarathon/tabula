package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.RenderMitCircsMessageAttachmentCommand._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesMessage
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object RenderMitCircsMessageAttachmentCommand {
  type Result = Option[RenderableAttachment]
  type Command = Appliable[Result] with RenderMitCircsMessageAttachmentState
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Read

  def apply(message: MitigatingCircumstancesMessage, filename: String): Command =
    new RenderMitCircsMessageAttachmentCommandInternal(message, filename)
      with ComposableCommand[Result]
      with RenderMitCircsMessageAttachmentPermissions
      with RenderMitCircsMessageAttachmentDescription
}

abstract class RenderMitCircsMessageAttachmentCommandInternal(val message: MitigatingCircumstancesMessage, val filename: String)
  extends CommandInternal[Result]
    with RenderMitCircsMessageAttachmentState {

  override def applyInternal(): Option[RenderableAttachment] =
    message.attachments
      .filter(_.hasData)
      .find(_.name == filename)
      .map(a => new RenderableAttachment(a))

}

trait RenderMitCircsMessageAttachmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RenderMitCircsMessageAttachmentState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission, mandatory(message.submission))
}

trait RenderMitCircsMessageAttachmentDescription extends Describable[Result] {
  self: RenderMitCircsMessageAttachmentState =>

  override lazy val eventName: String = "RenderMitCircsMessageAttachment"

  override def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(message.submission)
     .property("mitCircsMessage", message.id)
     .property("filename", filename)

  override def describeResult(d: Description, result: Option[RenderableAttachment]): Unit =
    d.property("fileFound", result.nonEmpty)
}

trait RenderMitCircsMessageAttachmentState {
  val message: MitigatingCircumstancesMessage
  val filename: String
}
