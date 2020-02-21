package uk.ac.warwick.tabula.commands.profiles.profile

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}

object ViewProfileCommand {
  type Command = Appliable[Member] with ViewProfilePermissionsRestriction

  def apply(profile: Member, viewer: CurrentUser): Command =
    new ViewProfileCommandInternal(MemberOrUser(profile), viewer)
      with ViewProfilePermissions
      with ViewProfilePermissionsRestriction
      with AutowiringSecurityServiceComponent
      with ComposableCommand[Member] // Init late, PermissionsChecking needs the autowired services
      with Unaudited with ReadOnly

  def stale(profile: Member, viewer: CurrentUser): Command =
    new ViewProfileCommandInternal(MemberOrUser(profile), viewer)
      with ViewStaleProfilePermissions
      with ViewProfilePermissionsRestriction
      with AutowiringSecurityServiceComponent
      with ComposableCommand[Member] // Init late, PermissionsChecking needs the autowired services
      with Unaudited with ReadOnly
}

abstract class ViewProfileCommandInternal(val profile: MemberOrUser, val viewer: CurrentUser)
  extends CommandInternal[Member]
    with PermissionsCheckingMethods
    with ViewProfileCommandState {

  override def applyInternal(): Member = mandatory(profile.asMember)
}

trait ViewProfileCommandState {
  def profile: MemberOrUser
  def viewer: CurrentUser
}

trait ViewProfilePermissionsRestriction extends RequiresPermissionsChecking with PermissionsCheckingMethods with Logging {
  self: ViewProfileCommandState
    with SecurityServiceComponent =>

  def permission: Permission

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    if (profile.isMember) {
      p.PermissionCheck(permission, mandatory(profile.asMember))
    } else {
      p.PermissionCheck(Permissions.UserPicker)
    }

    lazy val viewingOwnProfile = viewer.apparentUser.getWarwickId == profile.universityId
    lazy val viewerInSameDepartment = Option(viewer.apparentUser.getDepartmentCode)
      .map(_.toLowerCase)
      .exists { deptCode =>
        if (profile.isMember) mandatory(profile.asMember).touchedDepartments.map(_.code).contains(deptCode)
        else profile.departmentCode == deptCode
      }

    lazy val canSeeOtherDepartments: Boolean = profile.isMember && securityService.can(viewer, Permissions.Profiles.Read.CoreCrossDepartment, mandatory(profile.asMember))

    /*
     * Deny access over and above permissions if:
     * - You're not a god user
     * - You're not viewing your own profile
     * - You're not a member of staff
     * - The profile is for a student or applicant
     * - You don't explicitly have Profiles.Read.CoreCrossDepartment
     * - The profile's touchedDepartments doesn't include the user's department code
     */
    if (!viewer.god && !viewingOwnProfile && !viewer.isStaff && profile.asMember.exists(m => m.isStudent || m.isApplicant) && !canSeeOtherDepartments && !viewerInSameDepartment) {
      logger.info(s"Denying access for $viewer to view a student or applicant profile in a different department: $profile")
      throw PermissionDeniedException(viewer, Permissions.Profiles.Read.CoreCrossDepartment, profile.asMember.getOrElse(profile.asUser))
    }
  }
}

trait ViewProfilePermissions extends ViewProfilePermissionsRestriction {
  self: ViewProfileCommandState
    with SecurityServiceComponent =>

  val permission: Permission = Permissions.Profiles.Read.Core
}

trait ViewStaleProfilePermissions extends ViewProfilePermissionsRestriction {
  self: ViewProfileCommandState
    with SecurityServiceComponent =>

  val permission: Permission = Permissions.Profiles.Read.CoreStale
}
