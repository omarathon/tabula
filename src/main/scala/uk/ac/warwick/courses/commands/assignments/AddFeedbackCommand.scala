package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import scala.reflect.BeanProperty
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.courses.CurrentUser
import java.util.{List=>JList}
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.data.Daoisms
import org.springframework.web.multipart.MultipartFile
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FileDao
import scala.reflect.BeanInfo

/**
 * Command which (currently) adds a single piece of feedback for one assignment
 */
@Configurable
class AddFeedbackCommand( val assignment:Assignment, val submitter:CurrentUser ) extends Command[Feedback] with Daoisms {

  @Autowired var fileDao:FileDao =_
	
  @BeanProperty var uploadedFile:MultipartFile =_
	
  @BeanProperty var file:FileAttachment =_
  @BeanProperty var uniNumber:String =_
  
  @Transactional
  override def apply() = {
	  val feedback = new Feedback
	  feedback.uploaderId = submitter.apparentId
	  fileDao.save(file)
	  feedback.attachments add file
	  session.save(feedback)
	  feedback
  }

  def describe(d: Description) = d.properties(
		 "assignment" -> assignment.id
  )
  
  

}