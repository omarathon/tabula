package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.Submitter
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.roles.FeedbackRecipient
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.Member

/**
 * A special multi-purpose role provider that provides users access to their own data, generally this isn't an explicit permission.
 */
@Component
class OwnDataRoleProvider extends RoleProvider {

	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Seq[Role] = {		
		scope match {
			// You can view your own submission			
			case submission: Submission => 
				if (submission.universityId == user.universityId) Seq(Submitter(submission))
				else Seq()
				
			// You can view feedback to your work, but only if it's released
			case feedback: Feedback => 
				if (feedback.universityId == user.universityId && feedback.released) Seq(FeedbackRecipient(feedback))
				else Seq()
			
			case _ => Seq()
		}
	}
	
	def rolesProvided = Set(classOf[Submitter], classOf[FeedbackRecipient])

}