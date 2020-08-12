package uk.ac.warwick.tabula.data.model.notifications.cm2

import org.joda.time.{DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.data.model.{Assignment, Notification, UserGroup}
import uk.ac.warwick.tabula.data.model.markingworkflow.{MarkingWorkflowStage, SingleMarkerWorkflow}
import uk.ac.warwick.tabula.services.{ExtensionService, FeedbackService}
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}

import scala.jdk.CollectionConverters._

class ReturnToMarkerNotificationTest extends TestBase with Mockito with FreemarkerRendering {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()

  val recipient = Fixtures.user(userId = "cuscav")
  val department = Fixtures.department("in")
  val module = Fixtures.module("in101")
  module.adminDepartment = department

  val userLookup = new MockUserLookup
  userLookup.registerUserObjects(recipient)

  private trait Fixture {
    val assignment = Fixtures.assignment("demo")
    assignment.module = module
    assignment.closeDate = new LocalDate(2018, DateTimeConstants.SEPTEMBER, 11).toDateTime(Assignment.defaultCloseTime)
    assignment.extensionService = smartMock[ExtensionService]
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty
    assignment.cm2MarkingWorkflow = SingleMarkerWorkflow("test", assignment.module.adminDepartment, Seq(recipient))
    assignment.cm2MarkingWorkflow.stageMarkers.asScala.foreach(_.markers.asInstanceOf[UserGroup].userLookup = userLookup)

    val student = Fixtures.user("9999991", "9999992")

    val feedback = Fixtures.assignmentFeedback("9999991", "9999992")
    feedback.assignment = assignment

    assignment.feedbackService = smartMock[FeedbackService]
    assignment.feedbackService.loadFeedbackForAssignment(assignment) returns Seq(feedback)

    val markerFeedback = Fixtures.markerFeedback(feedback)
    markerFeedback.userLookup = userLookup
    markerFeedback.marker = recipient
    markerFeedback.stage = MarkingWorkflowStage.SingleMarker
  }

  @Test def single(): Unit = new Fixture {
    val notification = Notification.init(new ReturnToMarkerNotification, recipient, markerFeedback, assignment)
    notification.userLookup = userLookup
    notification.recipientUserId = recipient.getUserId

    notification.titleFor(recipient) should be ("IN101: Submissions for demo have been returned to you")

    val content = notification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      s"""1 submission for demo - IN101 has been returned to you.
         |
         |Student feedback is due on 9 October 2018.
         |""".stripMargin
    )

    notification.comment.value = "I just think you should do it again."

    val content2 = notification.content
    renderToString(freeMarkerConfig.getTemplate(content2.template), content2.model) should be (
      s"""1 submission for demo - IN101 has been returned to you.
         |
         |The following reason was given: I just think you should do it again.
         |
         |Student feedback is due on 9 October 2018.
         |""".stripMargin
    )

    assignment.openEnded = true

    val content3 = notification.content
    renderToString(freeMarkerConfig.getTemplate(content3.template), content3.model) should be (
      s"""1 submission for demo - IN101 has been returned to you.
         |
         |The following reason was given: I just think you should do it again.
         |""".stripMargin
    )
  }

  @Test def batch(): Unit = new Fixture {
    val notification1 = Notification.init(new ReturnToMarkerNotification, recipient, markerFeedback, assignment)
    notification1.userLookup = userLookup
    notification1.recipientUserId = recipient.getUserId
    notification1.comment.value = "comment1"

    val feedback2 = Fixtures.assignmentFeedback("9999993", "9999994")
    feedback2.assignment = assignment

    val markerFeedback2 = Fixtures.markerFeedback(feedback2)
    markerFeedback2.userLookup = userLookup
    markerFeedback2.marker = recipient
    markerFeedback2.stage = MarkingWorkflowStage.SingleMarker

    val notification2 = Notification.init(new ReturnToMarkerNotification, recipient, markerFeedback2, assignment)
    notification2.userLookup = userLookup
    notification2.recipientUserId = recipient.getUserId
    notification2.comment.value = "comment2"

    val assignment2 = Fixtures.assignment("demo2")
    assignment2.module = module
    assignment2.openEnded = true
    assignment2.extensionService = smartMock[ExtensionService]
    assignment2.extensionService.getApprovedExtensionsByUserId(assignment2) returns Map.empty
    assignment2.cm2MarkingWorkflow = SingleMarkerWorkflow("test", assignment2.module.adminDepartment, Seq(recipient))
    assignment2.cm2MarkingWorkflow.stageMarkers.asScala.foreach(_.markers.asInstanceOf[UserGroup].userLookup = userLookup)

    val feedback3 = Fixtures.assignmentFeedback("9999993", "9999994")
    feedback3.assignment = assignment

    val markerFeedback3 = Fixtures.markerFeedback(feedback3)
    markerFeedback3.userLookup = userLookup
    markerFeedback3.marker = recipient
    markerFeedback3.stage = MarkingWorkflowStage.SingleMarker

    val notification3 = Notification.init(new ReturnToMarkerNotification, recipient, markerFeedback3, assignment2)
    notification3.userLookup = userLookup
    notification3.recipientUserId = recipient.getUserId
    notification3.comment.value = "comment3"

    val batch = Seq(notification1, notification2, notification3)

    ReturnToMarkerBatchedNotificationHandler.titleForBatch(batch, recipient) should be ("Submissions for 2 assignments have been returned to you")

    val content = ReturnToMarkerBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be(
      s"""2 submissions for demo - IN101 have been returned to you.
         |
         |The following reasons were given:
         |
         |- comment1
         |- comment2
         |
         |Student feedback is due on 9 October 2018.
         |
         |---
         |
         |1 submission for demo2 - IN101 has been returned to you.
         |
         |The following reason was given:
         |
         |- comment3
         |""".stripMargin)
  }

}
