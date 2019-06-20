package uk.ac.warwick.tabula.data.model.notifications.mitcircs


import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.notifications.coursework.ExtensionFixture
import uk.ac.warwick.tabula.{Mockito, TestBase}

class NewMitCircsSubmissionNotificationTest extends TestBase with Mockito with MitCircsNotificationFixture {

  val n: NewMitCircsSubmissionNotification = {
    val n = Notification.init(new NewMitCircsSubmissionNotification, student, submission, submission)
    n.permissionsService = mockPermissionsService
    n
  }

  @Test
  def urlIsReviewPage(): Unit = {
    n.url should be(s"/mitcircs/submission/${submission.key}")
  }

  @Test
  def titleShouldContainMessage(): Unit = {
    n.title should be("New mitigating circumstances submission received")
  }

  @Test
  def recipientsContainsSingleUser(): Unit = {
    n.recipients should be(Seq(admin))
  }

  @Test
  def shouldCallTextRendererWithCorrectTemplate(): Unit = {
    n.content.template should be("/WEB-INF/freemarker/emails/new_mit_circs_submission.ftl")
  }

  @Test
  def shouldCallTextRendererWithCorrectModel(): Unit = new ExtensionFixture {
    n.content.model("submission") should be(submission)
  }

}
