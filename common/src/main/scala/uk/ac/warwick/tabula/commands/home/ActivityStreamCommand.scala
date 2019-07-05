package uk.ac.warwick.tabula.commands.home

import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services.ActivityService.PagedActivities
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, PubliclyVisiblePermissions, RequiresPermissionsChecking}

object ActivityStreamCommand {
  type Result = PagedActivities
  type Command = Appliable[Result] with ActivityStreamCommandState

  def apply(request: ActivityStreamRequest, user: CurrentUser): Command =
    new ActivityStreamCommandInternal(request, user)
      with ComposableCommand[Result]
      with ActivityStreamPermissions
      with Unaudited with ReadOnly
}

abstract class ActivityStreamCommandInternal(
  val request: ActivityStreamRequest,
  val user: CurrentUser,
) extends CommandInternal[PagedActivities]
  with ActivityStreamCommandState
  with NotificationServiceComponent {

  def applyInternal(): PagedActivities = transactional(readOnly = true) {
    val results = notificationService.stream(request)
    PagedActivities(
      results.items,
      results.lastUpdatedDate,
      results.totalHits
    )
  }
}

trait ActivityStreamPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ActivityStreamCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    // Check that the user is logged in (i.e. that it has dismiss notifications)
    // This is granted on the global scope
    p.PermissionCheck(Permissions.Notification.Dismiss, null)

    // Check that the current user matches the user in the request
    if (user.apparentId != request.user.getUserId)
      throw new PermissionDeniedException(user, Seq(Permissions.Notification.Dismiss), request.user)
  }
}

trait ActivityStreamCommandState {
  def request: ActivityStreamRequest
  def user: CurrentUser
}
