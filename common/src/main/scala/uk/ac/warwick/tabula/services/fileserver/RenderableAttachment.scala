package uk.ac.warwick.tabula.services.fileserver

import com.google.common.io.ByteSource
import com.google.common.net.MediaType
import uk.ac.warwick.tabula.data.model.FileAttachment

class RenderableAttachment(attachment: FileAttachment, name: Option[String] = None) extends RenderableFile {
	override def byteSource: ByteSource = if (attachment == null) null else attachment.asByteSource
	override def filename: String = attachment.name
	override def contentType: String = attachment.asByteSource.metadata.map(_.contentType).getOrElse(MediaType.OCTET_STREAM.toString)
	override def contentLength: Option[Long] = attachment.length

	override def suggestedFilename: Option[String] = name
}