package uk.ac.warwick.tabula.commands.profiles.profile

import org.springframework.validation.Errors

import scala.jdk.CollectionConverters._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringSecurityServiceComponent, ProfileService, ProfileServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}

object ViewMultipleProfileCommand {
  type Command = Appliable[Seq[Member]] with ViewMultipleProfilePermissions

  def apply(members: JList[String], viewer: CurrentUser): Command =
    new ViewMultipleProfilesCommandInternal(members, viewer)
      with ViewMultipleProfilePermissions
      with AutowiringSecurityServiceComponent
      with AutowiringProfileServiceComponent
      with ComposableCommand[Seq[Member]] // Init late, PermissionsChecking needs the autowired services
      with Unaudited with ReadOnly

}

abstract class ViewMultipleProfilesCommandInternal(val members: JList[String], val viewer: CurrentUser)
  extends CommandInternal[Seq[Member]]
    with PermissionsCheckingMethods
    with ProfileServiceComponent
    with ViewMultipleProfilesCommandState {

  override def applyInternal(): Seq[Member] = memberObjects
}

trait ViewMultipleProfilesValidator extends SelfValidating {
  this: ViewMultipleProfilesCommandState =>

  override def validate(errors: Errors): Unit = {
    if (memberObjects.headOption.isEmpty) {
      errors.reject("NotEmpty")
    }
  }
}

trait ViewMultipleProfilesCommandState {
  self: ProfileServiceComponent =>

  def members: JList[String]
  lazy val memberObjects: Seq[Member] = profileService.getAllMembersWithUniversityIds(members.asScala.toSeq)
  def viewer: CurrentUser
}

trait ViewMultipleProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods with Logging with CheckViewProfile {
  self: ViewMultipleProfilesCommandState
    with SecurityServiceComponent  =>


  val permission: Permission = Permissions.Profiles.Read.Core

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    for (member <- memberObjects) {
      checkViewProfile(p, MemberOrUser(member), viewer)
    }
  }
}