package uk.ac.warwick.courses.commands.assignments

import scala.reflect.BeanInfo
import scala.reflect.BeanProperty
import org.hibernate.validator.constraints.NotEmpty
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Errors
import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.data.FileDao
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.util.core.StringUtils.hasText
import uk.ac.warwick.courses.UniversityId
import collection.JavaConversions._
import uk.ac.warwick.courses.services.ZipService
import org.springframework.beans.factory.annotation.Autowired

/**
 * Command which (currently) adds a single piece of feedback for one assignment
 */
@Configurable
class AddFeedbackCommand( val assignment:Assignment, val submitter:CurrentUser ) extends Command[Feedback] with Daoisms {

  @Autowired var zipService:ZipService =_
	
  @NotEmpty
  @BeanProperty var uniNumber:String =_
	
  @BeanProperty var file:UploadedFile = new UploadedFile
			
  // called manually by controller
  def validation(errors:Errors) = {
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
	  }
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
	  session.saveOrUpdate(feedback)
	  
	  // delete feedback zip for this assignment, since it'll now be different.
	  // TODO should really do this in a more general place, like a save listener for Feedback objects
	  zipService.invalidateFeedbackZip(assignment)
	  
	  feedback
  }

  def describe(d: Description) = d.assignment(assignment).properties(
	  "studentId" -> uniNumber
  )

}