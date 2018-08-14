package uk.ac.warwick.tabula.commands.profiles.profile

import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringSecurityServiceComponent
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}
import uk.ac.warwick.tabula.permissions.Permissions._

class ViewProfileCommand(user: CurrentUser, profile: Member)
	extends ViewViewableCommand(Permissions.Profiles.Read.Core, profile) with AutowiringSecurityServiceComponent with Logging {

	private val viewingOwnProfile = user.apparentUser.getWarwickId == profile.universityId
	private val viewerInSameDepartment = Option(user.apparentUser.getDepartmentCode)
		.map(_.toLowerCase)
		.exists(deptCode => profile.touchedDepartments.map(_.code).contains(deptCode))

	private def canSeeOtherDepartments: Boolean = securityService.can(user, Profiles.Read.CoreCrossDepartment, profile)

	if (!user.god && !viewingOwnProfile && (user.isStudent || profile.isStaff && !canSeeOtherDepartments && !viewerInSameDepartment)) {
		logger.info("Denying access for user " + user + " to view profile " + profile)
		throw PermissionDeniedException(user, Permissions.Profiles.Read.Core, profile)
	}
}
