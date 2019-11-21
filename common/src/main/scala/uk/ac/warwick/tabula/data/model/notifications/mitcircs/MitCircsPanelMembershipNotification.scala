package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.data.model.notifications.mitcircs.MitCircsPanelMembershipNotification._
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickNotification, Notification, SingleItemNotification}
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.userlookup.User

object MitCircsPanelMembershipNotification {
  val addedTemplateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_added_to_panel.ftlh"
  val removedTemplateLocation: String = "/WEB-INF/freemarker/emails/mit_circs_removed_from_panel.ftlh"
}


abstract class MitCircsPanelMembershipNotification
  extends Notification[MitigatingCircumstancesPanel, Unit]
    with SingleItemNotification[MitigatingCircumstancesPanel]
    with MyWarwickNotification {

  @transient var modifiedUsers: Seq[User] = Nil

  override def recipients: Seq[User] = modifiedUsers
}

@Entity
@Proxy
@DiscriminatorValue("MitCircsAddedToPanel")
class MitCircsAddedToPanelNotification extends MitCircsPanelMembershipNotification {

  override def verb: String = "added"

  override def title: String = s"You have been added to a mitigating circumstances panel ${item.entity.name}"

  override def content: FreemarkerModel = FreemarkerModel(addedTemplateLocation, Map(
    "panel" -> item.entity
  ))

  override def url: String = Routes.Admin.Panels.view(item.entity)

  override def urlTitle: String = "view the mitigating circumstances panel"
}

@Entity
@Proxy
@DiscriminatorValue("MitCircsRemovedFromPanel")
class MitCircsRemovedFromPanelNotification extends MitCircsPanelMembershipNotification {

  override def verb: String = "removed"

  override def title: String = s"You have been removed from a mitigating circumstances panel ${item.entity.name}"

  override def content: FreemarkerModel = FreemarkerModel(removedTemplateLocation, Map(
    "panel" -> item.entity
  ))

  override def url: String = Routes.home

  override def urlTitle: String = "view your mitigating circumstances dashboard"
}
