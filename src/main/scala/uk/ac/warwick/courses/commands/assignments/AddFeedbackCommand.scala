package uk.ac.warwick.courses.commands.assignments

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.{List => JList}
import scala.collection.JavaConversions._
import collection.JavaConverters._
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
import uk.ac.warwick.courses.services.ZipEntryInputStream
import uk.ac.warwick.courses.services.ZipService
import uk.ac.warwick.courses.services.Zips
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.UniversityId
import uk.ac.warwick.util.core.StringUtils.hasText
import uk.ac.warwick.util.core.spring.FileUtils
import scala.util.matching.Regex
import uk.ac.warwick.courses.helpers._
import uk.ac.warwick.userlookup.UserLookup
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.courses.services.UserLookupService
import java.util.ArrayList

class FeedbackItem {
	@BeanProperty var uniNumber:String =_
    @BeanProperty var file:UploadedFile = new UploadedFile
    
    def this(uniNumber:String) = {
		this();
		this.uniNumber = uniNumber;
	}
}

// Purely for storing in command to display on the model.
case class ProblemFile(
		@BeanProperty val path:String, 
		@BeanProperty val file:FileAttachment) {
	def this() = this(null,null)
}

// Purely to generate an audit log event
class ExtractFeedbackZip(cmd:AddFeedbackCommand) extends Command[Unit] {
	def apply() {}
	def describe(d:Description) = d.assignment(cmd.assignment).properties(
			"archive" -> cmd.archive.getOriginalFilename()
		)
}

/**
 * Command which adds feedback for an assignment.
 * It either takes a single uniNumber and file, or it takes a 
 * zip of files with uni numbers embedded in their path.
 */
@Configurable
class AddFeedbackCommand( val assignment:Assignment, val submitter:CurrentUser ) extends Command[List[Feedback]] with Daoisms with Logging {
	
//  val directoryPattern = new Regex("""*.*?(\d{7}))*.*?/([^/]+)""")
//  val filePattern = new Regex("""(\d{7}) - ([^/]+)""")
//  val anyFilePattern = new Regex("""(?:.*?/)?([^/]+)""")
  
  val uniNumberPattern = new Regex("""(\d{7,})""")
	
  @Autowired var zipService:ZipService =_
  @Autowired var userLookup:UserLookupService =_
  @Autowired var fileDao:FileDao =_
	
  /* for single upload */
  @BeanProperty var uniNumber:String =_
  @BeanProperty var file:UploadedFile = new UploadedFile
  /* ----- */

  /* for multiple upload */
  // use lazy list with factory as spring doesn't know how to dynamically create items 
  @BeanProperty var items:JList[FeedbackItem] = LazyLists.simpleFactory()
  @BeanProperty var unrecognisedFiles:JList[ProblemFile] = LazyLists.simpleFactory()
  @BeanProperty var invalidFiles:JList[ProblemFile] = LazyLists.simpleFactory()
  @BeanProperty var archive:MultipartFile = _
  @BeanProperty var batch:Boolean = false
  @BeanProperty var fromArchive:Boolean = false
  @BeanProperty var confirmed:Boolean = false
  /* ---- */
  
  private def filenameOf(path:String) = new java.io.File(path).getName
  
  def preExtractValidation(errors:Errors) = {
	  if (batch) {
		  if (archive != null && !archive.isEmpty()) {
		 	  logger.info("file name is " + archive.getOriginalFilename())
		 	  if (!"zip".equals(FileUtils.getLowerCaseExtension(archive.getOriginalFilename))) {
		 	 	  errors.rejectValue("archive", "archive.notazip")
		 	  }
		  } else if (items != null && items.isEmpty() && file.isMissing) {
		 	  errors.rejectValue("file.upload", "file.missing")
		  }
	  }
  }
  
  def postExtractValidation(errors:Errors) = {
	  if (!invalidFiles.isEmpty()) errors.rejectValue("invalidFiles", "invalidFiles")
	  if (items != null && !items.isEmpty()) {
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
	 	 	  userLookup.getUserByWarwickUniId(uniNumber) match {
	 	 	 	  case FoundUser(u) => 
	 	 	 	  case NoUser(u) => errors.rejectValue("uniNumber", "uniNumber.userNotFound", Array(uniNumber), "")
	 	 	  }
	 	 	  
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
	
	def store(itemMap:collection.mutable.Map[String,FeedbackItem], number:String, name:String, file:FileAttachment) =
		itemMap.getOrElseUpdate(number, new FeedbackItem(uniNumber=number))
			.file.attached.add(file)
		
	
	def processFiles(bits:Seq[Pair[String,FileAttachment]]) {
		// go through individual files, extracting the uni number and grouping
		// them into feedback items.
		var itemMap = new collection.mutable.HashMap[String,FeedbackItem]()
		unrecognisedFiles.clear()
		
		for ((filename, file) <- bits) {
			// match uni numbers found in file path
			val allNumbers = uniNumberPattern.findAllIn(filename).matchData.map{_.subgroups(0)}.toList
			
			// ignore any numbers longer than 7 characters.
			val numbers = allNumbers.filter { _.length == 7 }
			
			if (numbers.isEmpty) {
				// no numbers at all.
				unrecognisedFiles.add(new ProblemFile(filename, file))
			} else if (numbers.distinct.size > 1) {
				// multiple different numbers, ambiguous, reject this. 
				invalidFiles.add(new ProblemFile(filename, file))
			} else {
				// one 7 digit number, this one might be okay.
				store(itemMap, numbers.head, filenameOf(filename), file)
			}
		}
		
		items = new ArrayList(itemMap.values().toList)
	}
	
	// ZIP has been uploaded. unpack it
	if (archive != null && !archive.isEmpty()) {
		val zip = new ZipInputStream(archive.getInputStream)
		
		val bits = Zips.iterator(zip) { (iterator) =>
			for (entry <- iterator if !entry.isDirectory) yield {
				val f = new FileAttachment
				f.name = filenameOf(entry.getName)
				f.uploadedData = new ZipEntryInputStream(zip, entry)
				f.uploadedDataLength = entry.getSize
				fileDao.saveTemporary(f)
				(entry.getName, f)
			}
		}
		
		processFiles(bits)
		
		// remember we got these items from a Zip, so we can tailor the text in the HTML.
		fromArchive = true
		
		// this do-nothing command is to generate an audit event to record the unzipping
		new ExtractFeedbackZip(this).apply()
		
	} else {
		if (batch && !file.attached.isEmpty()) {
			val bits = file.attached.map { (attachment) => attachment.name -> attachment }
			processFiles(bits)
		}
		
		if (items != null) {
			for (item <- items if item.file != null) item.file.onBind
		}
	} 
	
	
  }
  
  @Transactional
  override def apply(): List[Feedback] = {
	  
	  def saveFeedback(uniNumber:String, file:UploadedFile)= {
	 	  val feedback = new Feedback
		  feedback.assignment = assignment
		  feedback.uploaderId = submitter.apparentId
		  feedback.universityId = uniNumber
		  for (attachment <- file.attached) 
			  feedback addAttachment attachment
		  session.saveOrUpdate(feedback)
		  
		  feedback
	  }
	  
	  if (items != null && !items.isEmpty()) {
	 	  
	 	  val feedbacks = items.map { (item) =>
	 	 	  saveFeedback(item.uniNumber, item.file)
	 	  }
	 	  zipService.invalidateFeedbackZip(assignment)
	 	  feedbacks.toList
	 	  
	  } else {
	  
	 	  val feedback = saveFeedback(uniNumber, file)
		  
		  // delete feedback zip for this assignment, since it'll now be different.
		  // TODO should really do this in a more general place, like a save listener for Feedback objects
		  zipService.invalidateFeedbackZip(assignment)
		  
		  List(feedback)
  	  }
  }
  
//  def filenameOf(entry:ZipEntry):String = {
//		entry.getName match {
//			case directoryPattern(number, name) => name
//			case filePattern(number, name) => name
//			case anyFilePattern(name) => name 
//		}
//	}

  def describe(d: Description) = d
		  .assignment(assignment)
		  .studentIds(items.map{ _.uniNumber })

}

