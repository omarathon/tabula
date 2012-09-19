package uk.ac.warwick.courses.data.convert
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FileDao
import scala.reflect.BeanProperty
import org.springframework.core.convert.converter.Converter
import uk.ac.warwick.courses.data.model.FileAttachment
import org.springframework.format.Formatter
import java.util.Locale

class FileAttachmentIdConverter extends Converter[String, FileAttachment] with Formatter[FileAttachment] {
	@Autowired @BeanProperty var fileDao: FileDao = _

	override def convert(code: String) = fileDao.getFileById(code).orNull

	override def parse(code: String, locale: Locale): FileAttachment = convert(code)
	override def print(attachment: FileAttachment, locale: Locale): String = attachment.id
}