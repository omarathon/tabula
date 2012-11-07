package uk.ac.warwick.courses.services.fileserver
import uk.ac.warwick.courses.data.model.FileAttachment
import java.io.File

class RenderableAttachment(attachment: FileAttachment) extends RenderableFile {
	override def inputStream = attachment.dataStream
	override def filename = attachment.name
	override def contentType = "application/octet-stream" // TODO mime type.
	override def contentLength = attachment.length
	override def file = Option(attachment.file)
}