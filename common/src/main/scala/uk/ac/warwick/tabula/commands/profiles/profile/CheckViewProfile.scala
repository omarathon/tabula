package uk.ac.warwick.tabula.commands.profiles.profile
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.SecurityServiceComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

trait CheckViewProfile extends RequiresPermissionsChecking with PermissionsCheckingMethods with SecurityServiceComponent {

  def permission: Permission

  def checkViewProfile(p: PermissionsChecking, profileToCheck: MemberOrUser, viewer: CurrentUser): Unit = {
    if (profileToCheck.isMember) {
      p.PermissionCheck(permission, mandatory(profileToCheck.asMember))
    } else {
      p.PermissionCheck(Permissions.UserPicker)
    }

    lazy val viewingOwnprofileToCheck = viewer.apparentUser.getWarwickId == profileToCheck.universityId
    lazy val viewerInSameDepartment = Option(viewer.apparentUser.getDepartmentCode)
      .map(_.toLowerCase)
      .exists { deptCode =>
        if (profileToCheck.isMember) mandatory(profileToCheck.asMember).touchedDepartments.map(_.code).contains(deptCode)
        else profileToCheck.departmentCode == deptCode
      }

    lazy val canSeeOtherDepartments: Boolean = profileToCheck.isMember && securityService.can(viewer, Permissions.Profiles.Read.CoreCrossDepartment, mandatory(profileToCheck.asMember))

    /*
     * Deny access over and above permissions if:
     * - You're not a god user
     * - You're not viewing your own profileToCheck
     * - You're not a member of staff
     * - The profileToCheck is for a student or applicant
     * - You don't explicitly have profileToChecks.Read.CoreCrossDepartment
     * - The profileToCheck's touchedDepartments doesn't include the user's department code
     */
    if (!viewer.god && !viewingOwnprofileToCheck && !viewer.isStaff && profileToCheck.asMember.exists(m => m.isStudent || m.isApplicant) && !canSeeOtherDepartments && !viewerInSameDepartment) {
      logger.info(s"Denying access for $viewer to view a student or applicant profileToCheck in a different department: $profileToCheck")
      throw PermissionDeniedException(viewer, Permissions.Profiles.Read.CoreCrossDepartment, profileToCheck.asMember.getOrElse(profileToCheck.asUser))
    }
  }
}
