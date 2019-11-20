package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.mitcircs.submission.MitCircsShareSubmissionCommand._
import uk.ac.warwick.tabula.commands.permissions._
import uk.ac.warwick.tabula.commands.{ComposableCommand, Notifies}
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.{MitCircsSubmissionAddSharingNotification, MitCircsSubmissionRemoveSharingNotification}
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.roles.{MitigatingCircumstancesViewerRoleDefinition, RoleDefinition}
import uk.ac.warwick.tabula.services.mitcircs.AutowiringMitCircsSubmissionServiceComponent
import uk.ac.warwick.tabula.services.permissions.AutowiringPermissionsServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringUserLookupComponent, SecurityServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object MitCircsShareSubmissionCommand {
  type AddCommand =
    GrantRoleCommand.Command[MitigatingCircumstancesSubmission]
      with MitCircsShareSubmissionState
      with Notifies[GrantRoleCommand.Result[MitigatingCircumstancesSubmission], MitigatingCircumstancesSubmission]

  type RemoveCommand =
    RevokeRoleCommand.Command[MitigatingCircumstancesSubmission]
      with MitCircsShareSubmissionState
      with Notifies[RevokeRoleCommand.Result[MitigatingCircumstancesSubmission], MitigatingCircumstancesSubmission]

  val roleDefinition: RoleDefinition = MitigatingCircumstancesViewerRoleDefinition
  val requiredPermission: Permission = Permissions.MitigatingCircumstancesSubmission.Share

  def add(submission: MitigatingCircumstancesSubmission, creator: User): AddCommand =
    new GrantRoleCommandInternal(submission)
      with ComposableCommand[GrantRoleCommand.Result[MitigatingCircumstancesSubmission]]
      with MitCircsShareSubmissionState
      with RoleCommandRequest
      with MitCircsShareSubmissionValidation
      with MitCircsShareSubmissionPermissions
      with MitCircsShareSubmissionAddDescription
      with MitCircsShareSubmissionAddNotifications
      with AutowiringPermissionsServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringUserLookupComponent {
      override val allowUnassignableRoles: Boolean = true
      override val roleDefinition: RoleDefinition = MitCircsShareSubmissionCommand.this.roleDefinition
      override val currentUser: User = creator
    }

  def remove(submission: MitigatingCircumstancesSubmission, creator: User): RemoveCommand =
    new RevokeRoleCommandInternal(submission)
      with ComposableCommand[RevokeRoleCommand.Result[MitigatingCircumstancesSubmission]]
      with MitCircsShareSubmissionState
      with RoleCommandRequest
      with RevokeRoleCommandValidation
      with MitCircsShareSubmissionPermissions
      with MitCircsShareSubmissionRemoveDescription
      with MitCircsShareSubmissionRemoveNotifications
      with AutowiringPermissionsServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringUserLookupComponent
      with AutowiringMitCircsSubmissionServiceComponent {
      override val allowUnassignableRoles: Boolean = true
      override val roleDefinition: RoleDefinition = MitCircsShareSubmissionCommand.this.roleDefinition
      override val currentUser: User = creator
    }
}

trait MitCircsShareSubmissionValidation extends GrantRoleCommandValidation {
  self: RoleCommandState[MitigatingCircumstancesSubmission] with RoleCommandRequest with SecurityServiceComponent with UserLookupComponent =>

  override def validate(errors: Errors): Unit = {
    super.validate(errors)

    val students = userLookup.usersByUserIds(usercodes.asScala.toSeq).values.filter(_.isStudent)
    if(students.nonEmpty)
      errors.rejectValue("usercodes", "mitigatingCircumstances.submission.share.noStudents", Array(students.map(_.getFullName).mkString(", ")), "")
  }
}

trait MitCircsShareSubmissionState {
  def currentUser: User
}

trait MitCircsShareSubmissionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RoleCommandState[MitigatingCircumstancesSubmission] =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(requiredPermission, mandatory(scope))
  }
}

trait MitCircsShareSubmissionAddDescription extends GrantRoleCommandDescription[MitigatingCircumstancesSubmission] {
  self: RoleCommandRequest
    with RoleCommandState[MitigatingCircumstancesSubmission] =>

  override lazy val eventName: String = "MitCircsShareSubmissionAdd"
}

trait MitCircsShareSubmissionRemoveDescription extends RevokeRoleCommandDescription[MitigatingCircumstancesSubmission] {
  self: RoleCommandRequest
    with RoleCommandState[MitigatingCircumstancesSubmission] =>

  override lazy val eventName: String = "MitCircsShareSubmissionRemove"
}

trait MitCircsShareSubmissionAddNotifications extends Notifies[GrantRoleCommand.Result[MitigatingCircumstancesSubmission], MitigatingCircumstancesSubmission] {
  self: RoleCommandRequest
    with RoleCommandState[MitigatingCircumstancesSubmission]
    with MitCircsShareSubmissionState
    with UserLookupComponent =>

  def emit(grantedRole: GrantRoleCommand.Result[MitigatingCircumstancesSubmission]): Seq[Notification[MitigatingCircumstancesSubmission, Unit]] =
    Seq(
      Notification.init(new MitCircsSubmissionAddSharingNotification, currentUser, scope)
        .tap(_.modifiedUsers = userLookup.getUsersByUserIds(usercodes).asScala.values.toSeq)
    )
}

trait MitCircsShareSubmissionRemoveNotifications extends Notifies[RevokeRoleCommand.Result[MitigatingCircumstancesSubmission], MitigatingCircumstancesSubmission] {
  self: RoleCommandRequest
    with RoleCommandState[MitigatingCircumstancesSubmission]
    with MitCircsShareSubmissionState
    with UserLookupComponent =>

  def emit(grantedRole: RevokeRoleCommand.Result[MitigatingCircumstancesSubmission]): Seq[Notification[MitigatingCircumstancesSubmission, Unit]] =
    Seq(
      Notification.init(new MitCircsSubmissionRemoveSharingNotification, currentUser, scope)
        .tap(_.modifiedUsers = userLookup.getUsersByUserIds(usercodes).asScala.values.toSeq)
    )
}
