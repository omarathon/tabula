package uk.ac.warwick.tabula.data.convert
import scala.beans.BeanProperty

import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.system.TwoWayConverter

class FileAttachmentIdConverter extends TwoWayConverter[String, FileAttachment] {
	@Autowired var fileDao: FileDao = _

	override def convertRight(id: String) = fileDao.getFileById(id).orNull
	override def convertLeft(attachment: FileAttachment) = (Option(attachment) map {_.id}).orNull
}