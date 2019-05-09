package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.RenderMitCircsNoteAttachmentCommand._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesNote
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object RenderMitCircsNoteAttachmentCommand {
  type Result = Option[RenderableAttachment]
  type Command = Appliable[Result] with RenderMitCircsNoteAttachmentState
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Read

  def apply(note: MitigatingCircumstancesNote, filename: String): Command =
    new RenderMitCircsNoteAttachmentCommandInternal(note, filename)
      with ComposableCommand[Result]
      with RenderMitCircsNoteAttachmentPermissions
      with RenderMitCircsNoteAttachmentDescription
}

abstract class RenderMitCircsNoteAttachmentCommandInternal(val note: MitigatingCircumstancesNote, val filename: String)
  extends CommandInternal[Result]
    with RenderMitCircsNoteAttachmentState {

  override def applyInternal(): Option[RenderableAttachment] =
    note.attachments
      .filter(_.hasData)
      .find(_.name == filename)
      .map(a => new RenderableAttachment(a))

}

trait RenderMitCircsNoteAttachmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RenderMitCircsNoteAttachmentState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission, mandatory(note.submission))
}

trait RenderMitCircsNoteAttachmentDescription extends Describable[Result] {
  self: RenderMitCircsNoteAttachmentState =>

  override lazy val eventName: String = "RenderMitCircsNoteAttachment"

  override def describe(d: Description): Unit =
    d.mitigatingCircumstancesSubmission(note.submission)
     .property("mitCircsNote", note.id)
     .property("filename", filename)

  override def describeResult(d: Description, result: Option[RenderableAttachment]): Unit =
    d.property("fileFound", result.nonEmpty)
}

trait RenderMitCircsNoteAttachmentState {
  val note: MitigatingCircumstancesNote
  val filename: String
}
