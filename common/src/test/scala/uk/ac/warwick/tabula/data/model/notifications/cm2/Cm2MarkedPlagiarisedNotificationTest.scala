package uk.ac.warwick.tabula.data.model.notifications.cm2

import uk.ac.warwick.tabula.data.model.{Notification, StudentMember}
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class Cm2MarkedPlagiarisedNotificationTest extends TestBase with Mockito with FreemarkerRendering {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
  val mockUserLookup: UserLookupService = smartMock[UserLookupService]

  mockUserLookup.getUserByUserId("1412345") returns new User("1412345")
  mockUserLookup.getUserByUserId("1673477") returns new User("1673477")

  @Test def title() = withUser("cuscav", "cuscav") {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")

    val submission = Fixtures.submission("1412345", "1412345")
    submission.assignment = assignment

    val notification = Notification.init(new Cm2MarkedPlagiarisedNotification, currentUser.apparentUser, submission, assignment)
    notification.userLookup = mockUserLookup
    notification.title should be("CS118: A submission by 1412345 for \"5,000 word essay\" is suspected of plagiarism")
  }

  @Test def batched() = withUser("cuscav", "cuscav") {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")

    val submission1 = Fixtures.submission("1412345", "1412345")
    submission1.assignment = assignment

    val submission2 = Fixtures.submission("1673477", "1673477")
    submission2.assignment = assignment

    val notification1 = Notification.init(new Cm2MarkedPlagiarisedNotification, currentUser.apparentUser, submission1, assignment)
    val notification2 = Notification.init(new Cm2MarkedPlagiarisedNotification, currentUser.apparentUser, submission2, assignment)
    notification1.userLookup = mockUserLookup
    notification2.userLookup = mockUserLookup

    val batch = Seq(notification1, notification2)

    Cm2MarkedPlagiarisedBatchedNotificationHandler.titleForBatch(batch, currentUser.apparentUser) should be("CS118: 2 submissions for \"5,000 word essay\" are suspected of plagiarism")
    val content = Cm2MarkedPlagiarisedBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be(
      """2 submissions for the assignment '5,000 word essay' for CS118, Programming for Computer Scientists have been marked as plagiarised.
        |
        |- 1412345
        |- 1673477
        |""".stripMargin
    )

  }

}
