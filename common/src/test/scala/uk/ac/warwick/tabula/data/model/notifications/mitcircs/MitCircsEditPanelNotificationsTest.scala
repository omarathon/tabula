package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.mitcircs.submission.{EditMitCircsPanelNotifications, EditMitCircsPanelRequest, EditMitCircsPanelState}
import uk.ac.warwick.tabula.data.model.NamedLocation
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class MitCircsEditPanelNotificationsTest extends TestBase with Mockito {

  @Test
  def generateNotifications(): Unit = {
    val admin: User = Fixtures.user("admin", "admin").tap(_.setFullName("Cuppy Cup"))

    val userA = Fixtures.user("a", "a")
    val userB = Fixtures.user("b", "b")
    val userC = Fixtures.user("c", "c")

    val panel: MitigatingCircumstancesPanel = Fixtures.mitigatingCircumstancesPanel(userA, userB)
    panel.name = "Heron panel"
    panel.date = Some(DateTime.now)
    panel.date = Some(DateTime.now.plusHours(3))
    panel.location = Some(NamedLocation("Heron lake"))

    class TestNotifier(val user: User, val panel: MitigatingCircumstancesPanel)
      extends EditMitCircsPanelNotifications with EditMitCircsPanelState with EditMitCircsPanelRequest

    val notifications = new TestNotifier(admin, panel)

    val updatedPanel = Fixtures.mitigatingCircumstancesPanel(userB, userC).tap(p => {
      p.name = "Heron panel"
      p.addSubmission(Fixtures.mitigatingCircumstancesSubmission("heron", "terrifiedStudent"))
      p.location = Some(NamedLocation("A dank heron infested swamp"))
      p.date = Some(DateTime.now.plusDays(7))
      p.endDate = Some(DateTime.now.plusDays(7).plusHours(1))
    })

    val n  = notifications.emit(updatedPanel)
    n.size should be (3)

    val updateNotification = n.collect { case n: MitCircsPanelUpdatedNotification => n }.head
    updateNotification.nameChangedSetting.value should be (false)
    updateNotification.dateChangedSetting.value should be (true)
    updateNotification.locationChangedSetting.value should be (true)
    updateNotification.submissionsAddedSetting.value should be (true)
    updateNotification.submissionsRemovedSetting.value should be (false)
    updateNotification.existingViewers should be (Seq(userB))

    val addedNotification = n.collect { case n: MitCircsAddedToPanelNotification => n }.head
    addedNotification.modifiedUsers should be (Seq(userC))

    val removedNotification = n.collect { case n: MitCircsRemovedFromPanelNotification => n }.head
    removedNotification.modifiedUsers should be (Seq(userA))
  }

}
