package uk.ac.warwick.courses.commands
import org.springframework.web.multipart.MultipartFile
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FileDao
import uk.ac.warwick.courses.data.model.FileAttachment
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Configurable
import java.io.File
import java.util.{List => JList}
import uk.ac.warwick.courses.helpers.LazyLists
import uk.ac.warwick.courses.helpers.ArrayList
import collection.JavaConversions._
import collection.JavaConverters._

/**
 * Encapsulates an initially-uploaded MultipartFile with a reference to
 * a FileAttachment to hold on to the ID of a file on subsequent submissions,
 * so that you don't have to re-upload a file again.
 * 
 * When an HTML5 multiple file upload control is used, Spring can bind this
 * to an array or collection of MultipartFile objects. This is why UploadedFile
 * has a collection of MultipartFiles, rather than using a collection of UploadedFile
 * objects. 
 */
@Configurable
class UploadedFile(allowMultiple:Boolean = true) {
  @Autowired var fileDao:FileDao =_
  
  // files bound from an upload request, prior to being persisted
  @BeanProperty var upload:JList[MultipartFile] = ArrayList()
  
  // files that have been persisted - can be represented in forms by ID
  @BeanProperty var attached:JList[FileAttachment] = ArrayList()//LazyLists.simpleFactory()
  
  def isMissing = !isExists
  def isExists = hasUploads || hasAttachments
  
  def hasAttachments = attached != null && !attached.isEmpty()
  def hasUploads = (upload != null && !upload.isEmpty() && upload.filter{_.isEmpty}.isEmpty)
  def isUploaded = hasUploads
  					
  def onBind {
	  for (item <- attached) {
		  if (item != null && !item.temporary) {
		 	  throw new IllegalStateException("Non temporary file used")
		  } 
	  }
	  if (shouldPersist) {
	 	  attached.clear()
	 	  // convert MultipartFiles into FileAttachments
	 	  val newAttachments = for (item <- upload) yield {
	 	 	  val a = new FileAttachment
		 	  a.name = new File(item.getOriginalFilename()).getName
		 	  a.uploadedData = item.getInputStream
		 	  a.uploadedDataLength = item.getSize
		 	  fileDao.saveTemporary(a)
		 	  a
	 	  }
	 	  attached.addAll(newAttachments)
	  } else {
	 	  // sometimes we manually add FileAttachments with uploaded data to persist
	 	  for (item <- attached if item.uploadedData != null) 
 	 	 	  fileDao.saveTemporary(item)
	  }
	  
  }
  
  /**
   * If there are no persisted files already and we have at least one non-empty
   * multipart file here, then save them as persisted fileattachments.
   */
  private def shouldPersist = !hasAttachments && hasUploads
}