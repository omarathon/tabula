package uk.ac.warwick.tabula.commands

import java.io.File
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.beans.BeanProperty
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.BindingResult

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
class UploadedFile extends BindListener {
	var fileDao = Wire.auto[FileDao]

	var disallowedFilenames = Wire.property("${uploads.disallowedFilenames}")
	var disallowedPrefixes = Wire.property("${uploads.disallowedPrefixes}")
		
	// files bound from an upload request, prior to being persisted
	var upload: JList[MultipartFile] = ArrayList()

	// files that have been persisted - can be represented in forms by ID
	var attached: JList[FileAttachment] = ArrayList() //LazyLists.simpleFactory()

	def uploadedFileNames: Seq[String] = upload.map(file => file.getOriginalFilename()) filter (_ != "")
	def attachedFileNames: Seq[String] = attached.map(file => file.getName)
	def fileNames = uploadedFileNames ++ attachedFileNames

	def isMissing = !isExists
	def isExists = hasUploads || hasAttachments

	def size: Int = 
		if (hasAttachments) attached.size
		else if (hasUploads) permittedUploads.size
		else 0

	def attachedOrEmpty: JList[FileAttachment] = Option(attached) getOrElse ArrayList()
	def uploadOrEmpty: JList[MultipartFile] = permittedUploads

	def hasAttachments = attached != null && !attached.isEmpty()
	def hasUploads = !permittedUploads.isEmpty
	
	def permittedUploads: JList[MultipartFile] = {
		Option(upload).getOrElse(ArrayList()).filterNot { s => s.isEmpty || (disallowedFilenames.split(",").toList contains s.getOriginalFilename()) || disallowedPrefixes.split(",").toList.exists(s.getOriginalFilename().startsWith)}
	}
		
	def isUploaded = hasUploads

	/**
	 * Performs persistence of uploads such as converting MultipartFiles
	 * into FileAttachments saves to the database. When first saved, these
	 * FileAttachments are marked as "temporary" until persisted by whatever
	 * command needs them. This method will throw an exception
	 */
	override def onBind(result: BindingResult) {
		
		val bindResult = for (item <- attached) yield {
			if (item != null && !item.temporary) {
				result.reject("binding.reSubmission")
				false
			} else true
		}
		
		// Early exit if we've failed binding
		if (bindResult.find(_ == false).isDefined) return
		
		if (hasUploads) {
			// convert MultipartFiles into FileAttachments
			transactional() {
				val newAttachments = for (item <- permittedUploads) yield {
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
			}
		} else {
			// sometimes we manually add FileAttachments with uploaded data to persist
			for (item <- attached if item.uploadedData != null)
				fileDao.saveTemporary(item)
		}

	}
}