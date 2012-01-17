package uk.ac.warwick.courses.commands
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.validation.Errors

class PublishFeedbackCommand extends Command[Unit] {
  
	@BeanProperty var assignment:Assignment =_
  
	@BeanProperty var confirm:Boolean = false
	
	def validate(errors:Errors) {
	  if (!confirm) {
	    errors.rejectValue("confirm","feedback.publish.confirm")
	  }
	}
	
	def apply {
	  
	}
	
	def describe(d:Description) {}
}