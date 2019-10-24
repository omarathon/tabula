package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}

class DownloadSupportingFilesCommand(
  val module: Module,
  val assignment: Assignment,
  val extension: Extension,
  val filename: String)
  extends Command[Option[RenderableFile]] with ReadOnly {

  mustBeLinked(mandatory(assignment), mandatory(module))
  PermissionCheck(Permissions.Extension.Read, extension)

  private var fileFound: Boolean = _
  var callback: (RenderableFile) => Unit = _

  def applyInternal(): Option[RenderableAttachment] = {
    val allAttachments = extension.nonEmptyAttachments
    val attachment = allAttachments find (_.name == filename) map (a => new RenderableAttachment(a))

    fileFound = attachment.isDefined
    if (callback != null) {
      attachment.map {
        callback(_)
      }
    }
    attachment
  }

  override def describe(d: Description): Unit =
    d.assignment(assignment)
     .property("filename", filename)

  override def describeResult(d: Description): Unit =
    d.property("fileFound", fileFound)

}
