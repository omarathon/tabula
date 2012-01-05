package uk.ac.warwick.courses.commands.assignments

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.{List => JList}
import scala.collection.JavaConversions._
import scala.reflect.BeanInfo
import scala.reflect.BeanProperty
import org.apache.commons.collections.list.LazyList
import org.apache.commons.collections.FactoryUtils
import org.hibernate.validator.constraints.NotEmpty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Errors
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.FileDao
import uk.ac.warwick.courses.helpers.ArrayList
import uk.ac.warwick.courses.services.ZipEntryInputStream
import uk.ac.warwick.courses.services.ZipService
import uk.ac.warwick.courses.services.Zips
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.UniversityId
import uk.ac.warwick.util.core.StringUtils.hasText
import uk.ac.warwick.util.core.spring.FileUtils
import java.util.HashMap
import scala.util.matching.Regex
import uk.ac.warwick.courses.helpers.ArrayList
import uk.ac.warwick.courses.helpers.ArrayList
import uk.ac.warwick.courses.helpers.LazyLists
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.helpers.ArrayList

class FeedbackItem {
	@BeanProperty var uniNumber:String =_
    @BeanProperty var file:UploadedFile = new UploadedFile
}

/**
 * Command which (currently) adds a single piece of feedback for one assignment
 */
@Configurable
class AddFeedbackCommand( val assignment:Assignment, val submitter:CurrentUser ) extends Command[Feedback] with Daoisms with Logging {
	
  val directoryPattern = new Regex("""(\d{7})/([^/]+)""")
  val filePattern = new Regex("""(\d{7}) - ([^/]+)""")
  val anyFilePattern = new Regex(""".+([^/]+)""")
	
  @Autowired var zipService:ZipService =_
	
  /* for single upload */
  @BeanProperty var uniNumber:String =_
  @BeanProperty var file:UploadedFile = new UploadedFile
  /* ----- */

  /* for multiple upload */
  // use lazy list with factory as spring doesn't know how to dynamically create items 
  @BeanProperty var items:JList[FeedbackItem] = LazyLists.simpleFactory()
  @BeanProperty var unrecognisedFiles:JList[FileAttachment] = LazyLists.simpleFactory()
  @BeanProperty var archive:MultipartFile = _
  @BeanProperty var confirmed:Boolean = false
  /* ---- */
  
  // called manually by controller
  def validation(errors:Errors) = {
	  if (archive != null) {
	 	  logger.info("file name is " + archive.getOriginalFilename())
	 	  if (!"zip".equals(FileUtils.getLowerCaseExtension(archive.getOriginalFilename))) {
	 	 	  errors.rejectValue("archive", "archive.notazip")
	 	  }
	  } else if (items != null && !items.isEmpty()) {
	 	  for (i <- 0 until items.length) {
	 	 	  val item = items.get(i)
	 	 	  errors.pushNestedPath("items["+i+"]")
	 	 	  validateUploadedFile(item.file, item.uniNumber, errors)
	 	 	  errors.popNestedPath()
	 	  }
	  } else {
	 	  validateUploadedFile(file, uniNumber, errors)
	  }
  }
  
  private def validateUploadedFile(file:UploadedFile, uniNumber:String, errors:Errors) {
	  if (file isMissing) errors.rejectValue("file", "file.missing")
	  if (hasText(uniNumber)){
	 	  if (!UniversityId.isValid(uniNumber)) {
	 		  errors.rejectValue("uniNumber", "uniNumber.invalid")
	 	  } else {
	 	 	  // Reject if feedback for this student is already uploaded
	 	 	  assignment.feedbacks.find { _.universityId == uniNumber } match {
	 	 	 	  case Some(feedback) => errors.rejectValue("uniNumber", "uniNumber.duplicate.feedback")
	 	 	 	  case None => {}
	 	 	  }
	 	  }
	  } else {
	 	  errors.rejectValue("uniNumber", "NotEmpty")
	  }
  }
  
  @Transactional
  def onBind {
	file.onBind
	
	// any files that we don't understand will go in here to be displayed to the user.
	unrecognisedFiles.clear()
	
	// ZIP has been uploaded. unpack it
	if (archive != null) {
		val zip = new ZipInputStream(archive.getInputStream)
		
		val bits = Zips.iterator(zip) { (iterator) =>
			for (entry <- iterator if !entry.isDirectory) yield {
				val f = new FileAttachment
				f.name = filenameOf(entry)
				f.uploadedData = new ZipEntryInputStream(zip, entry)
				f.uploadedDataLength = entry.getSize
				
				val file = new UploadedFile
				file.attached = List(f)
				file.onBind
				
				(entry.getName, file)
			}
		}
		
		// go through individual files, extracting the uni number and grouping
		// them into feedback items.
		var itemMap = new HashMap[String,FeedbackItem]()
		
		def putItem(number:String, name:String, file:UploadedFile) {
			if (itemMap.containsKey(number)) {
				itemMap.get(number).file.attached.addAll(file.attached)
			} else {
				val item = new FeedbackItem
				item.uniNumber = number
				item.file.attached.addAll(file.attached)
				itemMap.put(number, item)
			}
		}
		
		for ((filename, file) <- bits) {
			filename match {
				case directoryPattern(number, name) => putItem(number, name, file)
				case filePattern(number, name) => putItem(number, name, file)
				case _ => unrecognisedFiles.addAll(file.attached)
			}
		}
		
		items = itemMap.values().toList
		
	} else if (items != null) {
		for (item <- items if item.file != null) item.file.onBind
	}
  }
  
  @Transactional
  override def apply() = {
	  val feedback = new Feedback
	  feedback.assignment = assignment
	  feedback.uploaderId = submitter.apparentId
	  feedback.universityId = uniNumber
	  for (attachment <- file.attached) 
		  feedback addAttachment attachment
	  session.saveOrUpdate(feedback)
	  
	  // delete feedback zip for this assignment, since it'll now be different.
	  // TODO should really do this in a more general place, like a save listener for Feedback objects
	  zipService.invalidateFeedbackZip(assignment)
	  
	  feedback
  }
  
  def filenameOf(entry:ZipEntry):String = {
		entry.getName match {
			case directoryPattern(number, name) => name
			case filePattern(number, name) => name
			case anyFilePattern(name) => name 
		}
	}

  def describe(d: Description) = d.assignment(assignment).properties(
	  "studentId" -> uniNumber
  )

}

