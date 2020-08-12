package uk.ac.warwick.tabula.data.model.notifications.cm2

import org.joda.time.{DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.data.model.markingworkflow.{MarkingWorkflowStage, SingleMarkerWorkflow}
import uk.ac.warwick.tabula.data.model.{Assignment, Notification, UserGroup}
import uk.ac.warwick.tabula.services.{CM2MarkingWorkflowService, ExtensionService, FeedbackService}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}

import scala.jdk.CollectionConverters._

class ReleaseToMarkerNotificationTest extends TestBase with Mockito with FreemarkerRendering {

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

  private trait CollectSubmissionsFixture extends Fixture {
    assignment.collectSubmissions = true

    val collectSubmissionsNotification = Notification.init(new ReleaseToMarkerNotification, recipient, markerFeedback, assignment)
    collectSubmissionsNotification.userLookup = userLookup
    collectSubmissionsNotification.recipientUserId = recipient.getUserId
    collectSubmissionsNotification.cm2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]
    collectSubmissionsNotification.cm2MarkingWorkflowService.getAllStudentsForMarker(assignment, recipient) returns Seq(student)
    collectSubmissionsNotification.cm2MarkingWorkflowService.getMarkerAllocations(assignment, MarkingWorkflowStage.SingleMarker) returns Map(recipient -> Set(student))
  }

  private trait NoCollectSubmissionsFixture extends Fixture {
    assignment.collectSubmissions = false

    val noCollectSubmissionsNotification = Notification.init(new ReleaseToMarkerNotification, recipient, markerFeedback, assignment)
    noCollectSubmissionsNotification.userLookup = userLookup
    noCollectSubmissionsNotification.recipientUserId = recipient.getUserId
    noCollectSubmissionsNotification.cm2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]
  }

  @Test def collectSubmissions(): Unit = new CollectSubmissionsFixture {
    collectSubmissionsNotification.titleFor(recipient) should be ("IN101: demo has been released for marking")

    val content = collectSubmissionsNotification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      s"""1 student is allocated to you for marking.
         |- Marker: 1 student
         |
         |1 student allocated to you has been released for marking:
         |- 0 students have submitted work that can be marked
         |- 0 students have not submitted but they have an extension
         |- 1 student has not submitted work and has not yet requested an extension
         |
         |Student feedback is due on 9 October 2018.
         |""".stripMargin
    )
  }

  @Test def noCollectSubmissions(): Unit = new NoCollectSubmissionsFixture {
    noCollectSubmissionsNotification.titleFor(recipient) should be ("IN101: demo has been released for marking")

    val content = noCollectSubmissionsNotification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      s"""1 student is allocated to you for marking.
         |
         |This assignment does not require students to submit work to Tabula.
         |""".stripMargin
    )
  }

  @Test def batch(): Unit = new CollectSubmissionsFixture { new NoCollectSubmissionsFixture {
    val batch = Seq(collectSubmissionsNotification, noCollectSubmissionsNotification)

    ReleaseToMarkerBatchedNotificationHandler.titleForBatch(batch, recipient) should be ("2 assignments have been released for marking")

    val content = ReleaseToMarkerBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be(
      s"""**IN101: demo has been released for marking:**
         |
         |1 student is allocated to you for marking.
         |- Marker: 1 student
         |
         |1 student allocated to you has been released for marking:
         |- 0 students have submitted work that can be marked
         |- 0 students have not submitted but they have an extension
         |- 1 student has not submitted work and has not yet requested an extension
         |
         |Student feedback is due on 9 October 2018.
         |
         |---
         |
         |**IN101: demo has been released for marking:**
         |
         |1 student is allocated to you for marking.
         |
         |This assignment does not require students to submit work to Tabula.
         |""".stripMargin)
  }}

}
