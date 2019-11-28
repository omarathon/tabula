package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.submission.EditMitCircsPanelCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesPanel, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.{MitCircsAddedToPanelNotification, MitCircsPanelUpdatedNotification, MitCircsRemovedFromPanelNotification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsPanelServiceComponent, MitCircsPanelServiceComponent}
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._
import scala.reflect.classTag

object EditMitCircsPanelCommand {
  type Result = MitigatingCircumstancesPanel
  type Command = Appliable[Result] with EditMitCircsPanelState with EditMitCircsPanelRequest with SelfValidating with PopulateOnForm
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesPanel.Modify

  def apply(panel: MitigatingCircumstancesPanel, user: User): Command =
    new EditMitCircsPanelCommandInternal(panel, user)
      with ComposableCommand[Result]
      with EditMitCircsPanelRequest
      with ModifyMitCircsPanelValidation
      with EditMitCircsPanelPermissions
      with EditMitCircsPanelDescription
      with EditMitCircsPanelPopulate
      with EditMitCircsPanelNotifications
      with AutowiringMitCircsPanelServiceComponent
      with AutowiringUserLookupComponent
      with AutowiringPermissionsServiceComponent
}

abstract class EditMitCircsPanelCommandInternal(val panel: MitigatingCircumstancesPanel, val user: User)
  extends CommandInternal[Result] with EditMitCircsPanelState {
  self: EditMitCircsPanelRequest with MitCircsPanelServiceComponent with UserLookupComponent with PermissionsServiceComponent =>

  override def applyInternal(): Result = transactional() {
    panel.name = name

    if (date != null) {
      panel.date = Option(start).map(date.toDateTime).orNull
      panel.endDate = Option(end).map(date.toDateTime).orNull
    } else {
      panel.date = null
      panel.endDate = null
    }

    if (locationId.hasText) {
      panel.location = MapLocation(location, locationId)
    } else if (location.hasText) {
      panel.location = NamedLocation(location)
    } else {
      panel.location = null
    }

    (panel.submissions -- submissions.asScala).foreach(panel.removeSubmission)
    submissions.asScala.foreach(panel.addSubmission)

    panel.chair = chair.maybeText.map(userLookup.getUserByUserId).orNull
    panel.secretary = secretary.maybeText.map(userLookup.getUserByUserId).orNull
    val viewers = (members.asScala.toSet ++ Set(chair, secretary)).filter(_.hasText)
    panel.viewers = viewers
    viewers.foreach { usercode =>
      permissionsService.clearCachesForUser((usercode, classTag[MitigatingCircumstancesPanel]))
    }

    mitCircsPanelService.saveOrUpdate(panel)
  }
}

trait EditMitCircsPanelPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: EditMitCircsPanelState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission, mandatory(panel))
}

trait EditMitCircsPanelDescription extends Describable[Result] {
  self: EditMitCircsPanelState =>

  override lazy val eventName: String = "EditMitCircsPanel"

  override def describe(d: Description): Unit =
    d.mitigatingCircumstancesPanel(panel)
}

trait EditMitCircsPanelPopulate extends PopulateOnForm {

  self: EditMitCircsPanelState with EditMitCircsPanelRequest =>

  override def populate(): Unit = {
    submissions.addAll((panel.submissions -- submissions.asScala.toSet).asJavaCollection)
  }
}

trait EditMitCircsPanelState {
  val user: User
  val panel: MitigatingCircumstancesPanel
}

trait EditMitCircsPanelRequest extends ModifyMitCircsPanelRequest {
  self: EditMitCircsPanelState =>

  val originalName: String = panel.name

  val originalDate: DateTime = panel.date
  val originalEndDate: DateTime = panel.endDate
  val originalLocation: Location = panel.location

  val originalSubmissions: Set[MitigatingCircumstancesSubmission] = panel.submissions
  val originalViewers: Set[User] = panel.viewers

  name = panel.name

  Option(panel.date).foreach { d =>
    date = d.toLocalDate
    start = d.toLocalTime

    Option(panel.endDate).foreach { e =>
      end = e.toLocalTime
    }
  }

  Option(panel.location).foreach {
    case NamedLocation(n) =>
      location = n

    case MapLocation(n, id, _) =>
      location = n
      locationId = id

    case AliasedMapLocation(_, MapLocation(n, id, _)) =>
      location = n
      locationId = id
  }

  Option(panel.chair).foreach(u => chair = u.getUserId)
  Option(panel.secretary).foreach(u => secretary = u.getUserId)

  members.clear()
  members.addAll(panel.members.map(_.getUserId).asJavaCollection)
}

trait EditMitCircsPanelNotifications extends Notifies[MitigatingCircumstancesPanel, MitigatingCircumstancesPanel] {

  self: EditMitCircsPanelRequest with EditMitCircsPanelState =>

  def emit(panel: MitigatingCircumstancesPanel): Seq[Notification[MitigatingCircumstancesPanel, Unit]] = {

    val nameChanged = panel.name != originalName
    val dateChanged = panel.date != originalDate || panel.endDate != originalEndDate
    val locationChanged = panel.location != originalLocation
    val submissionsAdded = panel.submissions.size > originalSubmissions.size
    val submissionsRemoved = panel.submissions.size < originalSubmissions.size

    val updateNotification = if(nameChanged || dateChanged || locationChanged || submissionsAdded || submissionsRemoved) {
      Seq(Notification.init(new MitCircsPanelUpdatedNotification, user, panel).tap(n => {
        n.existingViewers = (panel.viewers & originalViewers).toSeq
        n.nameChangedSetting.value = nameChanged
        n.dateChangedSetting.value = dateChanged
        n.locationChangedSetting.value = locationChanged
        n.submissionsAddedSetting.value = submissionsAdded
        n.submissionsRemovedSetting.value = submissionsRemoved
      }))
    } else Nil

    Seq(
      Notification.init(new MitCircsAddedToPanelNotification, user, panel).tap(_.modifiedUsers = (panel.viewers -- originalViewers).toSeq),
      Notification.init(new MitCircsRemovedFromPanelNotification, user, panel).tap(_.modifiedUsers = (originalViewers -- panel.viewers).toSeq),
    ) ++ updateNotification
  }
}