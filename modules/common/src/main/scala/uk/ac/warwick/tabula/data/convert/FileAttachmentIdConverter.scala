package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.system.TwoWayConverter

class FileAttachmentIdConverter extends TwoWayConverter[String, FileAttachment] {
	var fileDao = Wire[FileDao]

	override def convertRight(id: String) = fileDao.getFileById(id).orNull
	override def convertLeft(attachment: FileAttachment) = (Option(attachment) map {_.id}).orNull
}