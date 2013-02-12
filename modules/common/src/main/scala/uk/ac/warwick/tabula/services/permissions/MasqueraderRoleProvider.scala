package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.roles.Sysadmin
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.roles.Masquerader

@Component
class MasqueraderRoleProvider extends ScopelessRoleProvider {
	
	var userLookup = Wire.auto[UserLookupService]
	var masqueradeGroup: String = Wire.property("${permissions.masquerade.group}")
	
	def groupService = userLookup.getGroupService

	def getRolesFor(user: CurrentUser): Seq[Role] =
		if (user.realId.hasText && groupService.isUserInGroup(user.realId, masqueradeGroup)) Seq(Masquerader())
		else Seq()
		
	def rolesProvided = Set(classOf[Masquerader])
	
}