package uk.ac.warwick.courses.commands

import scala.collection.JavaConversions.asScalaBuffer
import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.core.StringUtils
import uk.ac.warwick.util.mail.WarwickMailSender
import javax.annotation.Resource
import uk.ac.warwick.courses.services.AssignmentService

@Configurable
class PublishFeedbackCommand extends Command[Unit] {
  
	@Resource(name="studentMailSender") var studentMailSender:WarwickMailSender =_
	//@Autowired var userLookup:UserLookupService =_
	@Autowired var assignmentService:AssignmentService =_
	
	@BeanProperty var assignment:Assignment =_
	@BeanProperty var module:Module =_
	
	@BeanProperty var confirm:Boolean = false
	
	@Value("${toplevel.url}") var topLevelUrl:String = _
	
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
	  val users = assignmentService.getUsersForFeedback(assignment)
	  for (info <- users) email(info)
	}
	
	private def email(info:Pair[String,User]) {
	  val (id,user) = info
	  if (user.isFoundUser()) {
	    val email = user.getEmail
	    if (StringUtils.hasText(email)) {
	      val message = messageFor(user)
	      studentMailSender.send(message)
	    } else {
	      emailErrors.reject("feedback.publish.user.noemail", Array(id), "")
	    }
	  } else {
	    emailErrors.reject("feedback.publish.user.notfound", Array(id), "")
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
	          
We thought you'd want to know that feedback is now available on your coursework '%s' for the module %s, %s. To retrieve this feedback, please visit:

%s

(Only you can retrieve this feedback, so you'll need your IT Services user name and password to confirm that it's really you.)

If you have any difficulty retrieving your feedback, please contact coursework@warwick.ac.uk. This email was sent from an automated system, and replies to it will not reach a real person.	          
""".format(
    		  user.getFirstName,
    		  assignment.name,
    		  assignment.module.code.toUpperCase,
    		  assignment.module.name,
    		  ("%s/module/%s/%s" format (topLevelUrl, assignment.module.code, assignment.id))
      ))
      return message
	}
	
	def describe(d:Description) = d
		.assignment(assignment)
		.studentIds(assignment.feedbacks.map { _.universityId })
}