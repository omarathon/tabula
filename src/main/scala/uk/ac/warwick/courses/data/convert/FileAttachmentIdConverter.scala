package uk.ac.warwick.courses.data.convert
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FileDao
import scala.reflect.BeanProperty
import org.springframework.core.convert.converter.Converter
import uk.ac.warwick.courses.data.model.FileAttachment

class FileAttachmentIdConverter extends Converter[String, FileAttachment] {
	@Autowired @BeanProperty var fileDao:FileDao =_
	
	override def convert(code:String) = fileDao.getFileById(code).orNull
}