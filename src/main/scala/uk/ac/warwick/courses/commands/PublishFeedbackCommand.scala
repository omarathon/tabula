package uk.ac.warwick.courses.commands
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.validation.Errors
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.data.model.Module
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.util.mail.WarwickMailSender
import collection.JavaConversions._
import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.userlookup.User
import org.springframework.validation.BindException
import org.hibernate.validator.constraints.impl.EmailValidator
import uk.ac.warwick.util.core.StringUtils
import org.springframework.mail.SimpleMailMessage

class PublishFeedbackCommand extends Command[Unit] {
  
	@Autowired var mailSender:WarwickMailSender =_
	@Autowired var userLookup:UserLookupService =_
	
	@BeanProperty var assignment:Assignment =_
	@BeanProperty var module:Module =_
	
	@BeanProperty var confirm:Boolean = false
	
	val fromAddress = "no-reply@warwick.ac.uk"
	
	var emailErrors:Errors =_
	
	// validation done even when showing initial form.
	def prevalidate(errors:Errors) {
	  if (assignment.closeDate.isAfterNow()) {
	    errors.rejectValue("assignment","feedback.publish.notclosed")
	  } else if (assignment.feedbacks.isEmpty()) {
	    errors.rejectValue("assignment","feedback.publish.nofeedback")
	  }
	}
	
	def validate(errors:Errors) {
	  prevalidate(errors)
	  if (!confirm) {
	    errors.rejectValue("confirm","feedback.publish.confirm")
	  }
	}
	
	@Transactional
	def apply {
	  assignment.resultsPublished = true
	  emailErrors = new BindException(this, "")
	  val uniIds = assignment.feedbacks.map { _.universityId }
	  val users = uniIds.map { userLookup.getUserByWarwickUniId(_) }
	  for (user <- users) email(user)
	}
	
	private def email(user:User) {
	  if (user.isFoundUser()) {
	    val email = user.getEmail
	    if (StringUtils.hasText(email)) {
	      val message = messageFor(user)
	      mailSender.send(message)
	    } else {
	      emailErrors.reject("feedback.publish.user.noemail", Array(user.getUserId))
	    }
	  } else {
	    emailErrors.reject("feedback.publish.user.notfound", Array(user.getUserId), "")
	  }
	}
	
	private def messageFor(user:User): SimpleMailMessage = {
	  val message = new SimpleMailMessage
      message.setFrom(fromAddress)
      message.setTo(user.getEmail)
      // TODO configurable subject
      message.setSubject("Your feedback is ready")
      // TODO configurable body (or at least, Freemarker this up)
      message.setText("""
Hello %s
	          
Your assignment has been marked and feedback is ready for you to download.
	          
%s
	          
	      """.format(
    		  user.getFirstName,
    		  "LINK"
      ))
      return message
	}
	
	def describe(d:Description) = d
		.assignment(assignment)
		.studentIds(assignment.feedbacks.map { _.universityId })
}