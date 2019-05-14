package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.notifications.coursework.ExtensionFixture
import uk.ac.warwick.tabula.{Mockito, TestBase}

class PendingEvidenceReminderNotificationTest extends TestBase with Mockito with MitCircsNotificationFixture {

  val n: PendingEvidenceReminderNotification = {
    val n = Notification.init(new PendingEvidenceReminderNotification, student, submission)
    n
  }

  @Test
  def urlIsReviewPage(): Unit = {
    n.url should be(s"/profiles/view/student/personalcircs/mitcircs/pendingevidence/${submission.key}")
  }

  @Test
  def titleShouldContainMessage(): Unit = {
    n.title should be(s"Mitigating circumstances evidence required for MIT-${submission.key}")
  }

  @Test
  def recipientsContainsSingleUser(): Unit = {
    n.recipients should be(Seq(student))
  }

  @Test
  def shouldCallTextRendererWithCorrectTemplate(): Unit = {
    n.content.template should be("/WEB-INF/freemarker/emails/pending_evidence_reminder.ftl")
  }

  @Test
  def shouldCallTextRendererWithCorrectModel(): Unit = new ExtensionFixture {
    n.content.model("submission") should be(submission)
  }

}
