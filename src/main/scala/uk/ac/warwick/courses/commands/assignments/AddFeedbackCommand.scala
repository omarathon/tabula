package uk.ac.warwick.courses.commands.assignments

import java.util.{List => JList}
import scala.reflect.BeanInfo
import scala.reflect.BeanProperty
import org.hibernate.validator.constraints.NotEmpty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.courses.validators._
import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.FileDao
import uk.ac.warwick.courses.CurrentUser
import java.io.File


/**
 * Command which (currently) adds a single piece of feedback for one assignment
 */
@Configurable
@SpelAsserts(Array(
	new SpelAssert(value="filePresent", message="{file.missing}")
))
class AddFeedbackCommand( val assignment:Assignment, val submitter:CurrentUser ) extends Command[Feedback] with Daoisms {
	
  @Autowired var fileDao:FileDao =_
	
  @BeanProperty var file:MultipartFile =_
  @BeanProperty var fileAttachment:FileAttachment = _
  def isFilePresent = (file != null && !file.isEmpty()) ||
  					fileAttachment != null
  
  @NotEmpty
  @BeanProperty var uniNumber:String =_
  
  @Transactional
  def onBind = {
	  if (fileAttachment == null && file != null && !file.isEmpty()) {
	 	  fileAttachment = new FileAttachment
	 	  fileAttachment.name = new File(file.getOriginalFilename()).getName
	 	  fileAttachment.uploadedData = file.getInputStream
	 	  fileAttachment.uploadedDataLength = file.getSize
	 	  fileDao.saveTemporary(fileAttachment)
	  }
  }
  
  @Transactional
  override def apply() = {
	  val feedback = new Feedback
	  feedback.uploaderId = submitter.apparentId
	  feedback.universityId = uniNumber
	  feedback.attachments add fileAttachment
	  session.save(feedback)
	  feedback
  }

  def describe(d: Description) = d.properties(
		 "assignment" -> assignment.id
  )

}