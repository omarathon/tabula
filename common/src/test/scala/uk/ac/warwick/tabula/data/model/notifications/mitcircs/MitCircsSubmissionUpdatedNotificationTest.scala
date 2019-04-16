package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.notifications.coursework.ExtensionFixture
import uk.ac.warwick.tabula.{Mockito, TestBase}

class MitCircsSubmissionUpdatedNotificationTest extends TestBase with Mockito with MitCircsNotificationFixture {

  val n: MitCircsSubmissionUpdatedNotification = {
    val n = Notification.init(new MitCircsSubmissionUpdatedNotification, student, submission, submission)
    n.permissionsService = mockPermissionsService
    n
  }

  @Test
  def urlIsReviewPage(): Unit = {
    n.url should be(s"/mitcircs/admin/review/${submission.key}")
  }

  @Test
  def titleShouldContainMessage(): Unit = {
    n.title should be(s"Mitigating circumstances submission MIT-${submission.key} updated")
  }

  @Test
  def recipientsContainsSingleUser(): Unit = {
    n.recipients should be(Seq(admin))
  }

  @Test
  def shouldCallTextRendererWithCorrectTemplate(): Unit = {
    n.content.template should be("/WEB-INF/freemarker/emails/mit_circs_submission_updated.ftl")
  }

  @Test
  def shouldCallTextRendererWithCorrectModel(): Unit = new ExtensionFixture {
    n.content.model("submission") should be(submission)
  }

}
