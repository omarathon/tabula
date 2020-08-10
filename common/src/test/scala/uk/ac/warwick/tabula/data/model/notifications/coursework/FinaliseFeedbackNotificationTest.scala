package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, FreemarkerModel, Notification}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.helpers.Tap._

class FinaliseFeedbackNotificationTest extends TestBase with Mockito with FreemarkerRendering {

  private trait Fixture {
    val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()

    val assignment: Assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
  }

  private trait SingleItemFixture extends Fixture {
    val feedback: Feedback = Fixtures.assignmentFeedback("0000001").tap(_.assignment = assignment)

    val notification: FinaliseFeedbackNotification = Notification.init(new FinaliseFeedbackNotification, currentUser.apparentUser, feedback, assignment)
  }

  private trait MultipleItemsFixture extends Fixture {
    val feedbacks: Seq[Feedback] =
      Seq("0000001", "0000002", "0000003").map { uniId => Fixtures.assignmentFeedback(uniId).tap(_.assignment = assignment)
    }

    val notification: FinaliseFeedbackNotification = Notification.init(new FinaliseFeedbackNotification, currentUser.apparentUser, feedbacks, assignment)
  }

  @Test def titleSingle = withUser("cuscav", "0672089") {
    new SingleItemFixture {
      notification.title should be("CS118: 1 submission for \"5,000 word essay\" has been marked")
    }
  }

  @Test def titleMultiple = withUser("cuscav", "0672089") {
    new MultipleItemsFixture {
      notification.title should be("CS118: 3 submissions for \"5,000 word essay\" have been marked")
    }
  }

  @Test def outputSingle = withUser("cuscav", "0672089") {
    new SingleItemFixture {
      val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
      notificationContent should be(
        """1 submission for CS118 5,000 word essay has been marked and is ready to be published to students:
          |
          |* 0000001
          |""".stripMargin
      )
    }
  }

  @Test def outputMultiple = withUser("cuscav", "0672089") {
    new MultipleItemsFixture {
      val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
      notificationContent should be(
        """3 submissions for CS118 5,000 word essay have been marked and are ready to be published to students:
          |
          |* 0000001
          |* 0000002
          |* 0000003
          |""".stripMargin
      )
    }
  }

  @Test def batch(): Unit = withUser("cuscav", "0672089") { new MultipleItemsFixture {
    // Two notifications, one with the first feedback and the second with the remainder

    val notification1: FinaliseFeedbackNotification = Notification.init(new FinaliseFeedbackNotification, currentUser.apparentUser, feedbacks.head, assignment)
    val notification2: FinaliseFeedbackNotification = Notification.init(new FinaliseFeedbackNotification, currentUser.apparentUser, feedbacks.tail, assignment)

    val batch = Seq(notification1, notification2)

    FinaliseFeedbackBatchedNotificationHandler.titleForBatch(batch, currentUser.apparentUser) should be ("CS118: 3 submissions for \"5,000 word essay\" have been marked")

    val content: FreemarkerModel = FinaliseFeedbackBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """3 submissions for CS118 5,000 word essay have been marked and are ready to be published to students:
        |
        |* 0000001
        |* 0000002
        |* 0000003
        |""".stripMargin
    )
  }}

}
