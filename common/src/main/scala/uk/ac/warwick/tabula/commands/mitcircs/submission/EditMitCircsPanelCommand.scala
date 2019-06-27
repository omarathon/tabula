package uk.ac.warwick.tabula.commands.mitcircs.submission

import uk.ac.warwick.tabula.commands.mitcircs.submission.EditMitCircsPanelCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.data.model.{AliasedMapLocation, MapLocation, NamedLocation}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsPanelServiceComponent, MitCircsPanelServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object EditMitCircsPanelCommand {
  type Result = MitigatingCircumstancesPanel
  type Command = Appliable[Result] with EditMitCircsPanelState with EditMitCircsPanelRequest with SelfValidating with PopulateOnForm
  val RequiredPermission: Permission = Permissions.MitigatingCircumstancesPanel.Modify

  def apply(panel: MitigatingCircumstancesPanel): Command =
    new EditMitCircsPanelCommandInternal(panel)
      with ComposableCommand[Result]
      with EditMitCircsPanelRequest
      with ModifyMitCircsPanelValidation
      with EditMitCircsPanelPermissions
      with EditMitCircsPanelDescription
      with EditMitCircsPanelPopulate
      with AutowiringMitCircsPanelServiceComponent
      with AutowiringUserLookupComponent
}

abstract class EditMitCircsPanelCommandInternal(val panel: MitigatingCircumstancesPanel)
  extends CommandInternal[Result]
    with EditMitCircsPanelState {
  self: EditMitCircsPanelRequest
    with MitCircsPanelServiceComponent
    with UserLookupComponent =>

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
    panel.viewers = members.asScala.toSet ++ Set(chair, secretary).filter(_.hasText)

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
  def panel: MitigatingCircumstancesPanel
}

trait EditMitCircsPanelRequest extends ModifyMitCircsPanelRequest {
  self: EditMitCircsPanelState =>

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
  Option(panel.chair).foreach(u => chair = u.getUserId)

  members.clear()
  members.addAll(panel.members.map(_.getUserId).asJavaCollection)
}