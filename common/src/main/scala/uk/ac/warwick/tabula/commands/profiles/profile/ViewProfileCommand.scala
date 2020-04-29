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

trait ViewProfilePermissionsRestriction extends RequiresPermissionsChecking with PermissionsCheckingMethods with Logging with CheckViewProfile {
  self: ViewProfileCommandState
    with SecurityServiceComponent =>

  def permission: Permission

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    val profileToCheck = profile
    checkViewProfile(p, profileToCheck, viewer)
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
