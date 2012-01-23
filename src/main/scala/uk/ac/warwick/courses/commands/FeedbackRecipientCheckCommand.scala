package uk.ac.warwick.courses.commands

import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.courses.helpers._
import uk.ac.warwick.courses.helpers.StringUtils._


case class RecipientCheckReport

@Configurable
class FeedbackRecipientCheckCommand extends Command[RecipientCheckReport] with Unaudited {
	
	@BeanProperty var assignment:Assignment =_
	@Autowired var assignmentService:AssignmentService =_
	//@Autowired var userLookup:UserLookupService =_
	
	override def apply = {
		val info = for (user <- assignmentService.getUsersForFeedback(assignment)) yield user match {
			case FoundUser(user) => {
				if (user.getEmail.hasText) {
					
				} else {
					
				}
			} 
			case NoUser(user) =>  
		}
		RecipientCheckReport()
	}
}