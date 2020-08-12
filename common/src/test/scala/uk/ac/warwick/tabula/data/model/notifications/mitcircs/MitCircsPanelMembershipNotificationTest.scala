package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, Notification}
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.userlookup.AnonymousUser

class MitCircsPanelMembershipNotificationTest extends TestBase with Mockito with FreemarkerRendering {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()

  private trait Fixture {
    val panel: MitigatingCircumstancesPanel = Fixtures.mitigatingCircumstancesPanel()
    panel.name = "Heron watching panel"
  }

  @Test def added(): Unit = new Fixture {
    val notification: MitCircsAddedToPanelNotification = Notification.init(new MitCircsAddedToPanelNotification, new AnonymousUser, panel)

    notification.titleFor(new AnonymousUser) should be ("You have been added to a mitigating circumstances panel Heron watching panel")

    val content: FreemarkerModel = notification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """You have been added as a member of a mitigating circumstances panel - Heron watching panel.
        |""".stripMargin
    )
  }

  @Test def addedBatch(): Unit = new Fixture {
    val notification1: MitCircsAddedToPanelNotification = Notification.init(new MitCircsAddedToPanelNotification, new AnonymousUser, panel)
    val notification2: MitCircsAddedToPanelNotification = Notification.init(new MitCircsAddedToPanelNotification, new AnonymousUser, panel)
    val notification3: MitCircsAddedToPanelNotification = Notification.init(new MitCircsAddedToPanelNotification, new AnonymousUser, panel)

    val batch = Seq(notification1, notification2, notification3)

    MitCircsAddedToPanelBatchedNotificationHandler.titleForBatch(batch, new AnonymousUser) should be ("You have been added to 3 mitigating circumstances panels")

    val content: FreemarkerModel = MitCircsAddedToPanelBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """You have been added as a member of the following mitigating circumstances panels:
        |
        |- Heron watching panel
        |- Heron watching panel
        |- Heron watching panel
        |""".stripMargin
    )
  }

  @Test def removed(): Unit = new Fixture {
    val notification: MitCircsRemovedFromPanelNotification = Notification.init(new MitCircsRemovedFromPanelNotification, new AnonymousUser, panel)

    notification.titleFor(new AnonymousUser) should be ("You have been removed from a mitigating circumstances panel Heron watching panel")

    val content: FreemarkerModel = notification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """You are no longer required to attend the mitigating circumstances panel - Heron watching panel.
        |""".stripMargin
    )
  }

  @Test def removedBatch(): Unit = new Fixture {
    val notification1: MitCircsRemovedFromPanelNotification = Notification.init(new MitCircsRemovedFromPanelNotification, new AnonymousUser, panel)
    val notification2: MitCircsRemovedFromPanelNotification = Notification.init(new MitCircsRemovedFromPanelNotification, new AnonymousUser, panel)
    val notification3: MitCircsRemovedFromPanelNotification = Notification.init(new MitCircsRemovedFromPanelNotification, new AnonymousUser, panel)

    val batch = Seq(notification1, notification2, notification3)

    MitCircsRemovedFromPanelBatchedNotificationHandler.titleForBatch(batch, new AnonymousUser) should be ("You have been removed from 3 mitigating circumstances panels")

    val content: FreemarkerModel = MitCircsRemovedFromPanelBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """You are no longer required to attend the following mitigating circumstances panels:
        |
        |- Heron watching panel
        |- Heron watching panel
        |- Heron watching panel
        |""".stripMargin
    )
  }

}
