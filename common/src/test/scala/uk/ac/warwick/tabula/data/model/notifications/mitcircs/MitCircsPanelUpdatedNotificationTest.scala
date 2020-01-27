package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.data.model.{NamedLocation, Notification}
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, MarkdownRendererImpl, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class MitCircsPanelUpdatedNotificationTest extends TestBase with Mockito with FreemarkerRendering with MarkdownRendererImpl {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()


  private trait MitCircsPanelUpdatedNotificationFixture {
    val admin: User = Fixtures.user("admin", "admin").tap(_.setFullName("Cuppy Cup"))

    val panel: MitigatingCircumstancesPanel = Fixtures.mitigatingCircumstancesPanel()
    panel.name = "Heron panel"
    panel.date = Some(DateTime.now)
    panel.date = Some(DateTime.now.plusHours(3))
    panel.location = Some(NamedLocation("Heron lake"))
  }

  @Test
  def nameChange(): Unit =  new MitCircsPanelUpdatedNotificationFixture {
    private val n = Notification.init(new MitCircsPanelUpdatedNotification, admin, panel).tap(n => {
      n.existingViewers = Seq(admin)
      n.nameChangedSetting.value = true
      n.dateChangedSetting.value = false
      n.locationChangedSetting.value = false
      n.submissionsAddedSetting.value = false
      n.submissionsRemovedSetting.value = false
    })

    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(n.content.template), n.content.model)
    notificationContent should be(
      """The mitigating circumstances panel has been updated.
        |
        |The panel is now called Heron panel.""".stripMargin
    )
  }

  @Test
  def lotsOfChanges(): Unit =  new MitCircsPanelUpdatedNotificationFixture {
    private val n = Notification.init(new MitCircsPanelUpdatedNotification, admin, panel).tap(n => {
      n.existingViewers = Seq(admin)
      n.nameChangedSetting.value = true
      n.dateChangedSetting.value = false
      n.locationChangedSetting.value = true
      n.submissionsAddedSetting.value = true
      n.submissionsRemovedSetting.value = false
    })

    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(n.content.template), n.content.model)
    notificationContent should be(
      """The mitigating circumstances panel has been updated.
        |
        |The panel is now called Heron panel.
        |The panel will now be held at Heron lake.
        |New mitigating circumstances submissions have been added to Heron panel.""".stripMargin
    )
  }

}
