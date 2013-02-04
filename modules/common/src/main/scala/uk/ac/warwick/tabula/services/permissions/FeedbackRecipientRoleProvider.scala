package uk.ac.warwick.tabula.services.permissions
import scala.collection.JavaConversions._

import org.springframework.stereotype.Component

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.FeedbackRecipient
import uk.ac.warwick.tabula.roles.Role

@Component
class FeedbackRecipientRoleProvider extends RoleProvider {
	
	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget): Seq[Role] = {		
		scope match {
			case feedback: Feedback => 
				if (feedback.universityId == user.universityId) Seq(FeedbackRecipient(feedback))
				else Seq()
			
			// FeedbackRecipient is only checked at the feedback level
			case _ => Seq()
		}
	}
	
	def rolesProvided = Set(classOf[FeedbackRecipient])
	
}