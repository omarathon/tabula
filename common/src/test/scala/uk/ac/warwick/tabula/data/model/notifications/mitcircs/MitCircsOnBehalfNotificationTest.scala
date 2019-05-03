package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.notifications.coursework.ExtensionFixture
import uk.ac.warwick.tabula.{Mockito, TestBase}

class MitCircsOnBehalfNotificationTest extends TestBase with Mockito with MitCircsNotificationFixture {

  val create: MitCircsSubmissionOnBehalfNotification = {
    val n = Notification.init(new MitCircsSubmissionOnBehalfNotification, student, submission, submission)
    n.userLookup = userLookup
    n.onPreSave(true)
    n
  }

  val update: MitCircsUpdateOnBehalfNotification = {
    val n = Notification.init(new MitCircsUpdateOnBehalfNotification, student, submission, submission)
    n.userLookup = userLookup
    n.onPreSave(true)
    n
  }

  @Test
  def urlIsReviewPage(): Unit = {
    create.url should be(s"/profiles/view/student/personalcircs/mitcircs/view/${submission.key}")
    update.url should be(s"/profiles/view/student/personalcircs/mitcircs/view/${submission.key}")
  }

  @Test
  def titleShouldContainMessage(): Unit = {
    create.title should be(s"Mitigating circumstances submission created on your behalf")
    update.title should be(s"Mitigating circumstances submission updated on your behalf")
  }

  @Test
  def recipientsContainsSingleUser(): Unit = {
    create.recipients should be(Seq(student))
    update.recipients should be(Seq(student))
  }

  @Test
  def shouldCallTextRendererWithCorrectTemplate(): Unit = {
    create.content.template should be("/WEB-INF/freemarker/emails/mit_circs_submission_on_behalf.ftl")
    update.content.template should be("/WEB-INF/freemarker/emails/mit_circs_submission_on_behalf.ftl")
  }

  @Test
  def shouldCallTextRendererWithCorrectModel(): Unit = new ExtensionFixture {
    create.content.model("action") should be("created")
    update.content.model("action") should be("updated")
  }

}
