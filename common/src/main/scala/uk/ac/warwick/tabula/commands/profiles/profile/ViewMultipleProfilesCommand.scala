package uk.ac.warwick.tabula.commands.profiles.profile

import org.springframework.validation.Errors

import scala.jdk.CollectionConverters._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, ProfileService, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}

object ViewMultipleProfileCommand {
  type Command = Appliable[JList[Member]] with ViewMultipleProfilesPermissionsRestriction

  def apply(members: JList[String], viewer: CurrentUser): Command =
    new ViewMultipleProfilesCommandInternal(members, viewer)
      with ViewMultipleProfilePermissions
      with ViewMultipleProfilesPermissionsRestriction
      with AutowiringSecurityServiceComponent
      with ComposableCommand[JList[Member]] // Init late, PermissionsChecking needs the autowired services
      with Unaudited with ReadOnly

}

abstract class ViewMultipleProfilesCommandInternal(val members: JList[String], val viewer: CurrentUser)
  extends CommandInternal[JList[Member]]
    with PermissionsCheckingMethods
    with ViewMultipleProfilesCommandState {


  override def applyInternal(): JList[Member] = service.getAllMembersWithUniversityIds(mandatory(members).asScala.toSeq)
}

trait ViewMultipleProfilesValidator extends SelfValidating {
  this: ViewMultipleProfilesCommandState =>

  var service: ProfileService = Wire.auto[ProfileService]

  override def validate(errors: Errors): Unit = {
    if (members == null || members.size() == 0) {
      errors.reject("NotEmpty")
    }
    memberObjects = service.getAllMembersWithUniversityIds(members.asScala.toSeq);
  }
}

trait ViewMultipleProfilesCommandState {
  def members: JList[String]
  var memberObjects: JList[Member] = _
  def viewer: CurrentUser
}

trait ViewMultipleProfilesPermissionsRestriction extends RequiresPermissionsChecking with PermissionsCheckingMethods with Logging {
  self: ViewMultipleProfilesCommandState
    with SecurityServiceComponent  =>

  var service: ProfileService = Wire.auto[ProfileService]

  def permission: Permission

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    for (member <- memberObjects.asScala) {
      p.PermissionCheck(permission, mandatory(member))
      lazy val viewingOwnProfile = viewer.apparentUser.getWarwickId == member.universityId
      lazy val viewerInSameDepartment = Option(viewer.apparentUser.getDepartmentCode)
        .map(_.toLowerCase)
        .exists { deptCode =>
          mandatory(member).touchedDepartments.map(_.code).contains(deptCode)
        }

      lazy val canSeeOtherDepartments: Boolean = securityService.can(viewer, Permissions.Profiles.Read.CoreCrossDepartment, mandatory(member))

      /*
       * See ViewProfilePermissionsRestriction
       */
      if (!viewer.god && !viewingOwnProfile && !viewer.isStaff && (member.isStudent || member.isApplicant) && !canSeeOtherDepartments && !viewerInSameDepartment) {
        logger.info(s"Denying access for $viewer to view a student or applicant profile in a different department: $member")
        throw PermissionDeniedException(viewer, Permissions.Profiles.Read.CoreCrossDepartment, member)
      }
    }

   
  }
}

trait ViewMultipleProfilePermissions extends ViewMultipleProfilesPermissionsRestriction {
  self: ViewMultipleProfilesCommandState
    with SecurityServiceComponent =>

  val permission: Permission = Permissions.Profiles.Read.Core
}
