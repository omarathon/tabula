package uk.ac.warwick.tabula.services.fileserver

import java.io.InputStream

import com.google.common.net.MediaType
import uk.ac.warwick.tabula.data.model.FileAttachment

class RenderableAttachment(attachment: FileAttachment) extends RenderableFile {
	override def inputStream: InputStream = if (attachment == null) null else attachment.asByteSource.openStream()
	override def filename: String = attachment.name
	override def contentType: String = attachment.asByteSource.metadata.map(_.contentType).getOrElse(MediaType.OCTET_STREAM.toString)
	override def contentLength: Option[Long] = attachment.length
}