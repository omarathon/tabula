package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.roles.SettingsOwner
import uk.ac.warwick.tabula.helpers.StringUtils._

@Component
class SettingsOwnerRoleProvider extends RoleProvider {
	
	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget): Seq[Role] =
		scope match {
			case settings: UserSettings => 
				if (user.loggedIn && user.apparentId.hasText && settings.userId == user.apparentId) Seq(SettingsOwner(settings))
				else Seq()
				
			case _ => Seq()
		}
	
	def rolesProvided = Set(classOf[SettingsOwner])

}