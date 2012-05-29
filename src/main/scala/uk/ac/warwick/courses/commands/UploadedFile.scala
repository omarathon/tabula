package uk.ac.warwick.courses.commands

import java.io.File

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.reflect.BeanProperty

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.web.multipart.MultipartFile

import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.data.model.FileAttachment
import uk.ac.warwick.courses.data.FileDao
import uk.ac.warwick.courses.helpers.ArrayList

/**
 * Encapsulates initially-uploaded MultipartFiles with a reference to
 * FileAttachments to hold on to the ID of a file on subsequent submissions,
 * so that you don't have to re-upload a file again.
 * 
 * When an HTML5 multiple file upload control is used, Spring can bind this
 * to an array or collection of MultipartFile objects. This is why UploadedFile
 * has a collection of MultipartFiles, rather than using a collection of UploadedFile
 * objects. 
 * 
 * Works well with the filewidget macro in forms.ftl. Remember to set the
 * multipart/form-data encoding type on your form.
 */
@Configurable
class UploadedFile {
  @Autowired var fileDao:FileDao =_
  
  // files bound from an upload request, prior to being persisted
  @BeanProperty var upload:JList[MultipartFile] = ArrayList()
  
  // files that have been persisted - can be represented in forms by ID
  @BeanProperty var attached:JList[FileAttachment] = ArrayList()//LazyLists.simpleFactory()
  
  def uploadedFileNames : Seq[String] = upload.map(file => file.getOriginalFilename()) filter (_ != "")
  def attatchedFileNames : Seq[String]  = attached.map(file => file.getName())
  def fileNames = uploadedFileNames ++ attatchedFileNames
  
  def isMissing = !isExists
  def isExists = hasUploads || hasAttachments
  
  def size:Int = if (hasAttachments) attached.size
  			else if (hasUploads) nonEmptyUploads.size
  			else 0
  
  def attachedOrEmpty = Option(attached) getOrElse ArrayList()
  def uploadOrEmpty = Option(upload) getOrElse ArrayList()
	  
  def hasAttachments = attached != null && !attached.isEmpty()
  def hasUploads = !nonEmptyUploads.isEmpty
  def nonEmptyUploads = Option(upload).getOrElse(ArrayList()).filterNot{_.isEmpty}
  def isUploaded = hasUploads
  
  /**
   * Performs persistence of uploads such as converting MultipartFiles
   * into FileAttachments saves to the database. When first saved, these
   * FileAttachments are marked as "temporary" until persisted by whatever
   * command needs them. This method will throw an exception
   */
  def onBind {
	  for (item <- attached) {
		  if (item != null && !item.temporary) {
		 	  throw new IllegalStateException("Non temporary file used")
		  } 
	  }
	  if (hasUploads) {    
	 	  // convert MultipartFiles into FileAttachments
	 	  val newAttachments = for (item <- upload) yield {
	 	 	  val a = new FileAttachment
		 	  a.name = new File(item.getOriginalFilename()).getName
		 	  a.uploadedData = item.getInputStream
		 	  a.uploadedDataLength = item.getSize
		 	  fileDao.saveTemporary(a)
		 	  a
	 	  }
	 	  // remove converted files from upload to avoid duplicates
	 	  upload.clear
	 	  attached.addAll(newAttachments)
	  } else {
	 	  // sometimes we manually add FileAttachments with uploaded data to persist
	 	  for (item <- attached if item.uploadedData != null) 
 	 	 	  fileDao.saveTemporary(item)
	  }
	  
  }
}