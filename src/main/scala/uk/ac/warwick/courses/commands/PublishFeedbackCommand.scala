package uk.ac.warwick.courses.commands
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.validation.Errors
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.data.model.Module

class PublishFeedbackCommand extends Command[Unit] {
  
	@BeanProperty var assignment:Assignment =_
	@BeanProperty var module:Module =_
	
	@BeanProperty var confirm:Boolean = false
	
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
	}
	
	def describe(d:Description) {}
}