package uk.ac.warwick.tabula.commands.mitcircs

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesStudent, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment

import scala.collection.JavaConverters._

object RenderMitCircsAttachmentCommand {
  def apply(submission: MitigatingCircumstancesSubmission, filename: String) = new RenderMitCircsAttachmentCommandInternal(submission, filename)
    with ComposableCommand[Option[RenderableAttachment]]
    with RenderMitCircsAttachmentPermissions
    with RenderMitCircsAttachmentDescription
}

class RenderMitCircsAttachmentCommandInternal(val submission: MitigatingCircumstancesSubmission, val filename: String)
  extends CommandInternal[Option[RenderableAttachment]] with RenderMitCircsAttachmentState {

  def applyInternal(): Option[RenderableAttachment] = submission.attachments.asScala
    .filter(_.hasData)
    .find(_.name == filename)
    .map(a => new RenderableAttachment(a))

}

trait RenderMitCircsAttachmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RenderMitCircsAttachmentState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.MitigatingCircumstancesSubmission.Modify, MitigatingCircumstancesStudent(submission.student))
  }
}

trait RenderMitCircsAttachmentDescription extends Describable[Option[RenderableAttachment]] {
  self: RenderMitCircsAttachmentState =>

  override lazy val eventName: String = "RenderMitCircsAttachment"

  override def describe(d: Description) {
    d.mitigatingCircumstancesSubmission(submission)
    d.property("filename", filename)
  }

  override def describeResult(d: Description, result: Option[RenderableAttachment]) {
    d.property("fileFound", result.isDefined)
  }
}

trait RenderMitCircsAttachmentState {
  val submission: MitigatingCircumstancesSubmission
  val filename: String
}