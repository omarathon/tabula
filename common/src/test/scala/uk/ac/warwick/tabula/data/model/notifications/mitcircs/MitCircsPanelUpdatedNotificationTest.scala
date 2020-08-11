package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.data.model.{NamedLocation, Notification}
import uk.ac.warwick.tabula.helpers.{DateBuilder, TimeBuilder}
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, MarkdownRendererImpl, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class MitCircsPanelUpdatedNotificationTest extends TestBase with Mockito with FreemarkerRendering with MarkdownRendererImpl {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
  freeMarkerConfig.setSharedVariable("dateBuilder", new DateBuilder)
  freeMarkerConfig.setSharedVariable("timeBuilder", new TimeBuilder)

  private trait MitCircsPanelUpdatedNotificationFixture {
    val admin: User = Fixtures.user("admin", "admin").tap(_.setFullName("Cuppy Cup"))

    val panel: MitigatingCircumstancesPanel = Fixtures.mitigatingCircumstancesPanel()
    panel.name = "Heron panel"
    panel.date = Some(new DateTime(2020, DateTimeConstants.JULY, 13, 15, 30, 0, 0))
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
    notificationContent.stripLineEnd should be(
      """The mitigating circumstances panel has been updated.
        |
        |- The panel is now called Heron panel.""".stripMargin
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
    notificationContent.stripLineEnd should be(
      """The mitigating circumstances panel has been updated.
        |
        |- The panel is now called Heron panel.
        |- The panel will now be held at Heron lake.
        |- New mitigating circumstances submissions have been added to Heron panel.""".stripMargin
    )

    renderMarkdown(notificationContent) should be (
      """<p>The mitigating circumstances panel has been updated.</p>
        |<ul><li>The panel is now called Heron panel.</li><li>The panel will now be held at Heron lake.</li><li>New mitigating circumstances submissions have been added to Heron panel.</li></ul>
        |""".stripMargin
    )
  }

  @Test
  def batch(): Unit = new MitCircsPanelUpdatedNotificationFixture {
    private val notification1 = Notification.init(new MitCircsPanelUpdatedNotification, admin, panel).tap(n => {
      n.existingViewers = Seq(admin)
      n.nameChangedSetting.value = true
      n.dateChangedSetting.value = false
      n.locationChangedSetting.value = true
      n.submissionsAddedSetting.value = true
      n.submissionsRemovedSetting.value = false
    })

    private val notification2 = Notification.init(new MitCircsPanelUpdatedNotification, admin, panel).tap(n => {
      n.existingViewers = Seq(admin)
      n.nameChangedSetting.value = false
      n.dateChangedSetting.value = true
      n.locationChangedSetting.value = false
      n.submissionsAddedSetting.value = false
      n.submissionsRemovedSetting.value = false
    })

    private val notification3 = Notification.init(new MitCircsPanelUpdatedNotification, admin, panel).tap(n => {
      n.existingViewers = Seq(admin)
      n.nameChangedSetting.value = true
      n.dateChangedSetting.value = false
      n.locationChangedSetting.value = false
      n.submissionsAddedSetting.value = false
      n.submissionsRemovedSetting.value = true
    })

    private val batch = Seq(notification1, notification2, notification3)
    MitCircsPanelUpdatedBatchedNotificationHandler.titleForBatch(batch, admin) should be ("Mitigating circumstances panel Heron panel updated")

    private val content = MitCircsPanelUpdatedBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """The mitigating circumstances panel has been updated.
        |
        |- The panel is now called Heron panel.
        |- The panel will now take place on Mon 13ᵗʰ July 2020: 15:30.
        |- The panel will now be held at Heron lake.
        |- New mitigating circumstances submissions have been added to Heron panel.
        |- Mitigating circumstances submissions have been removed from Heron panel.
        |""".stripMargin
    )
  }

}
