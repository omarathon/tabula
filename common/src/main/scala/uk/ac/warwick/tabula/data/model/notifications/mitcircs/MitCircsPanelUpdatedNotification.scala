package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.HasSettings.BooleanSetting
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.data.model.{BatchedNotification, BatchedNotificationHandler, FreemarkerModel, MyWarwickNotification, Notification, SingleItemNotification}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

object MitCircsPanelUpdatedNotification {
  val templateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_panel_updated.ftl"
}

@Entity
@Proxy
@DiscriminatorValue("MitCircsPanelUpdated")
class MitCircsPanelUpdatedNotification
  extends BatchedNotification[MitigatingCircumstancesPanel, Unit, MitCircsPanelUpdatedNotification](MitCircsPanelUpdatedBatchedNotificationHandler)
    with SingleItemNotification[MitigatingCircumstancesPanel]
    with MyWarwickNotification
    with AutowiringUserLookupComponent
    with Logging {

  // only notify viewers that existed before the change - newly added viewers will be seeing this for the first time so won't care about changes
  @transient var existingViewers: Seq[User] = Nil

  def nameChangedSetting: BooleanSetting = BooleanSetting("nameChanged", false)
  def dateChangedSetting: BooleanSetting = BooleanSetting("dateChanged", false)
  def locationChangedSetting: BooleanSetting = BooleanSetting("locationChanged", false)
  def submissionsAddedSetting: BooleanSetting = BooleanSetting("submissionsAdded", false)
  def submissionsRemovedSetting: BooleanSetting = BooleanSetting("submissionsRemoved", false)

  override def recipients: Seq[User] = existingViewers

  def verb = "updated"

  def panel: MitigatingCircumstancesPanel = item.entity

  def title: String = s"Mitigating circumstances panel ${panel.name} updated"

  def content: FreemarkerModel = FreemarkerModel(MitCircsPanelUpdatedNotification.templateLocation, Map(
    "panel" -> panel,
    "nameChanged" -> nameChangedSetting.value,
    "dateChanged" -> dateChangedSetting.value,
    "locationChanged" -> locationChangedSetting.value,
    "submissionsAdded" -> submissionsAddedSetting.value,
    "submissionsRemoved" -> submissionsRemovedSetting.value,
  ))

  def url: String = Routes.Admin.Panels.view(panel)

  def urlTitle = s"view this mitigating circumstances panel"

}

object MitCircsPanelUpdatedBatchedNotificationHandler extends BatchedNotificationHandler[MitCircsPanelUpdatedNotification] {
  // Only batch notifications for the same panel, grouping changes together
  override def groupBatchInternal(notifications: Seq[MitCircsPanelUpdatedNotification]): Seq[Seq[MitCircsPanelUpdatedNotification]] =
    notifications.groupBy(_.panel).values.toSeq

  override def titleForBatchInternal(notifications: Seq[MitCircsPanelUpdatedNotification], user: User): String =
    notifications.head.titleFor(user)

  override def contentForBatchInternal(notifications: Seq[MitCircsPanelUpdatedNotification]): FreemarkerModel = {
    val nameChanged = notifications.exists(_.nameChangedSetting.value)
    val dateChanged = notifications.exists(_.dateChangedSetting.value)
    val locationChanged = notifications.exists(_.locationChangedSetting.value)
    val submissionsAdded = notifications.exists(_.submissionsAddedSetting.value)
    val submissionsRemoved = notifications.exists(_.submissionsRemovedSetting.value)

    FreemarkerModel(notifications.head.content.template, Map(
      "panel" -> notifications.head.panel,
      "nameChanged" -> nameChanged,
      "dateChanged" -> dateChanged,
      "locationChanged" -> locationChanged,
      "submissionsAdded" -> submissionsAdded,
      "submissionsRemoved" -> submissionsRemoved,
    ))
  }

  override def urlForBatchInternal(notifications: Seq[MitCircsPanelUpdatedNotification], user: User): String =
    notifications.head.urlFor(user)

  override def urlTitleForBatchInternal(notifications: Seq[MitCircsPanelUpdatedNotification]): String =
    notifications.head.urlTitle
}
