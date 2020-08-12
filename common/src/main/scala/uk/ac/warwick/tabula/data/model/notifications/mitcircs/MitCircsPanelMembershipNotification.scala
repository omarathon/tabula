package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.MitCircsPanelMembershipNotification._
import uk.ac.warwick.tabula.data.model.{BatchedNotification, BatchedNotificationHandler, FreemarkerModel, MyWarwickNotification, Notification, SingleItemNotification}
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.userlookup.User

object MitCircsPanelMembershipNotification {
  val addedTemplateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_added_to_panel.ftl"
  val batchAddedTemplateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_added_to_panel_batch.ftl"
  val removedTemplateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_removed_from_panel.ftl"
  val batchRemovedTemplateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_removed_from_panel_batch.ftl"
}

abstract class MitCircsPanelMembershipNotification[A <: Notification[_, _]](bnh: BatchedNotificationHandler[A])
  extends BatchedNotification[MitigatingCircumstancesPanel, Unit, A](bnh)
    with SingleItemNotification[MitigatingCircumstancesPanel]
    with MyWarwickNotification {

  @transient var modifiedUsers: Seq[User] = Nil

  override def recipients: Seq[User] = modifiedUsers
}

@Entity
@Proxy
@DiscriminatorValue("MitCircsAddedToPanel")
class MitCircsAddedToPanelNotification
  extends MitCircsPanelMembershipNotification[MitCircsAddedToPanelNotification](MitCircsAddedToPanelBatchedNotificationHandler) {

  override def verb: String = "added"

  override def title: String = s"You have been added to a mitigating circumstances panel ${item.entity.name}"

  override def content: FreemarkerModel = FreemarkerModel(addedTemplateLocation, Map(
    "panel" -> item.entity
  ))

  override def url: String = Routes.Admin.Panels.view(item.entity)

  override def urlTitle: String = "view the mitigating circumstances panel"
}

object MitCircsAddedToPanelBatchedNotificationHandler extends BatchedNotificationHandler[MitCircsAddedToPanelNotification] {
  override def titleForBatchInternal(notifications: Seq[MitCircsAddedToPanelNotification], user: User): String =
    s"You have been added to ${notifications.size} mitigating circumstances panels"

  override def contentForBatchInternal(notifications: Seq[MitCircsAddedToPanelNotification]): FreemarkerModel =
    FreemarkerModel(batchAddedTemplateLocation, Map(
      "panels" -> notifications.map(_.item.entity)
    ))

  override def urlForBatchInternal(notifications: Seq[MitCircsAddedToPanelNotification], user: User): String =
    Routes.home

  override def urlTitleForBatchInternal(notifications: Seq[MitCircsAddedToPanelNotification]): String =
    "view your mitigating circumstances panels"
}

@Entity
@Proxy
@DiscriminatorValue("MitCircsRemovedFromPanel")
class MitCircsRemovedFromPanelNotification
  extends MitCircsPanelMembershipNotification[MitCircsRemovedFromPanelNotification](MitCircsRemovedFromPanelBatchedNotificationHandler) {

  override def verb: String = "removed"

  override def title: String = s"You have been removed from a mitigating circumstances panel ${item.entity.name}"

  override def content: FreemarkerModel = FreemarkerModel(removedTemplateLocation, Map(
    "panel" -> item.entity
  ))

  override def url: String = Routes.home

  override def urlTitle: String = "view your mitigating circumstances dashboard"
}

object MitCircsRemovedFromPanelBatchedNotificationHandler extends BatchedNotificationHandler[MitCircsRemovedFromPanelNotification] {
  override def titleForBatchInternal(notifications: Seq[MitCircsRemovedFromPanelNotification], user: User): String =
    s"You have been removed from ${notifications.size} mitigating circumstances panels"

  override def contentForBatchInternal(notifications: Seq[MitCircsRemovedFromPanelNotification]): FreemarkerModel =
    FreemarkerModel(batchRemovedTemplateLocation, Map(
      "panels" -> notifications.map(_.item.entity)
    ))

  override def urlForBatchInternal(notifications: Seq[MitCircsRemovedFromPanelNotification], user: User): String =
    Routes.home

  override def urlTitleForBatchInternal(notifications: Seq[MitCircsRemovedFromPanelNotification]): String =
    "view your mitigating circumstances panels"
}
