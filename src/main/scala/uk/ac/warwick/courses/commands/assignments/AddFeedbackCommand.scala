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
import org.springframework.validation.Errors


/**
 * Command which (currently) adds a single piece of feedback for one assignment
 */
@Configurable
class AddFeedbackCommand( val assignment:Assignment, val submitter:CurrentUser ) extends Command[Feedback] with Daoisms {
	
  @NotEmpty
  @BeanProperty var uniNumber:String =_
	
  @BeanProperty var file:UploadedFile = new UploadedFile
			
  // called manually by controller
  def validation(errors:Errors) = {
	  if (file isMissing) errors.rejectValue("file", "file.missing")
  }
  
  @Transactional
  def onBind {
	file.onBind
  }
  
  @Transactional
  override def apply() = {
	  file.attached.temporary = false
	  val feedback = new Feedback
	  feedback.assignment = assignment
	  feedback.uploaderId = submitter.apparentId
	  feedback.universityId = uniNumber
	  feedback addAttachment file.attached
	  session.save(feedback)
	  feedback
  }

  def describe(d: Description) = d.properties(
		 "assignment" -> assignment.id
  )

}