package uk.ac.warwick.tabula.services.fileserver

import com.google.common.net.MediaType
import uk.ac.warwick.tabula.data.model.FileAttachment

class RenderableAttachment(attachment: FileAttachment) extends RenderableFile {
	override def inputStream = if (attachment == null) null else attachment.dataStream
	override def filename = attachment.name
	override def contentType = MediaType.OCTET_STREAM.toString // TODO mime type.
	override def contentLength = attachment.length
}