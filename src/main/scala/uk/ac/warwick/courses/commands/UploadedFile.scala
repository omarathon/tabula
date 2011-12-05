package uk.ac.warwick.courses.commands
import org.springframework.web.multipart.MultipartFile
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FileDao
import uk.ac.warwick.courses.data.model.FileAttachment
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Configurable
import java.io.File

/**
 * Encapsulates an initially-uploaded MultipartFile with a reference to
 * a FileAttachment to hold on to the ID of a file on subsequent submissions,
 * so that you don't have to re-upload a file again.
 * 
 * When it comes to supporting an HTML5 multiple file input, the way binding
 * works means this'll probably need to still be a single item but with arrays
 * of MultipartFiles and FileAttachments. Any MultipartFiles found would append
 * to the end of any persisted FileAttachments. 
 */
@Configurable
class UploadedFile {
  @Autowired var fileDao:FileDao =_
  @BeanProperty var upload:MultipartFile =_
  @BeanProperty var attached:FileAttachment = _
  
  def isExists = (upload != null && !upload.isEmpty()) ||
  					attached != null
  def isMissing = !isExists
  					
  def isUploaded = attached != null
  					
  def onBind {
	  if (attached == null && upload != null && !upload.isEmpty()) {
	 	  attached = new FileAttachment
	 	  attached.name = new File(upload.getOriginalFilename()).getName
	 	  attached.uploadedData = upload.getInputStream
	 	  attached.uploadedDataLength = upload.getSize
	 	  fileDao.saveTemporary(attached)
	  }
  }
}