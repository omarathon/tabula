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

    if (!viewer.god && !viewingOwnProfile && (viewer.isStudent || profile.isStaff && !canSeeOtherDepartments && !viewerInSameDepartment)) {
      logger.info("Denying access for user " + viewer + " to view profile " + profile)
      throw PermissionDeniedException(viewer, permission, profile)
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
