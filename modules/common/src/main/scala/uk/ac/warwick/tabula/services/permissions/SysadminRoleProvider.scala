package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.roles.Sysadmin
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.util.core.StringUtils._

@Component
class SysadminRoleProvider extends ScopelessRoleProvider {
	
	var userLookup = Wire.auto[UserLookupService]
	var adminGroup: String = Wire.property("${permissions.admin.group}")
	
	def groupService = userLookup.getGroupService

	def getRolesFor(user: CurrentUser): Seq[Role] =
		if (hasText(user.realId) && groupService.isUserInGroup(user.realId, adminGroup)) Seq(Sysadmin())
		else Seq()
		
	def rolesProvided = Set(classOf[Sysadmin])
	
}