package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.roles.FeedbackRecipient
import uk.ac.warwick.tabula.roles.Submitter
import uk.ac.warwick.tabula.roles.SettingsOwner

/**
 * A special multi-purpose role provider that provides users access to their own data, generally this isn't an explicit permission.
 */
@Component
class OwnDataRoleProvider extends RoleProvider {

	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = {
		scope match {
			// You can view your own submission			
			case submission: Submission => 
				if (submission.universityId == user.universityId) Stream(Submitter(submission))
				else Stream.empty
				
			// You can view feedback to your work, but only if it's released
			case feedback: Feedback => 
				if (feedback.universityId == user.universityId && feedback.released) Stream(FeedbackRecipient(feedback))
				else Stream.empty
				
			// You can change your own user settings
			case settings: UserSettings => 
				if (user.apparentId.hasText && settings.userId == user.apparentId) Stream(SettingsOwner(settings))
				else Stream.empty

			//You can view small groups that you are a member of
			case smallGroup: SmallGroup => {
				val studentId = user.apparentUser.getWarwickId
				if (studentId.hasText && smallGroup.students.includesUser(user.apparentUser)) Stream(SmallGroupMember(smallGroup))
				else Stream.empty
			}

			case _ => Stream.empty
		}
	}
	
	def rolesProvided = Set(classOf[Submitter], classOf[FeedbackRecipient], classOf[SettingsOwner])

}