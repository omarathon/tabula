package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model.HasSettings.BooleanSetting
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickNotification, Notification, SingleItemNotification}
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
  extends Notification[MitigatingCircumstancesPanel, Unit]
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
