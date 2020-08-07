package uk.ac.warwick.tabula.data.model.notifications.cm2

import org.joda.time.{DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.data.model.markingworkflow.{MarkingWorkflowStage, SingleMarkerWorkflow}
import uk.ac.warwick.tabula.data.model.{Assignment, Notification, UserGroup}
import uk.ac.warwick.tabula.services.{CM2MarkingWorkflowService, ExtensionService, FeedbackService}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}

import scala.jdk.CollectionConverters._

class StopMarkingNotificationTest extends TestBase with Mockito with FreemarkerRendering {

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

    val collectSubmissionsNotification = Notification.init(new StopMarkingNotification, recipient, markerFeedback, assignment)
    collectSubmissionsNotification.userLookup = userLookup
    collectSubmissionsNotification.recipientUserId = recipient.getUserId
  }

  private trait NoCollectSubmissionsFixture extends Fixture {
    assignment.collectSubmissions = false

    val noCollectSubmissionsNotification = Notification.init(new StopMarkingNotification, recipient, markerFeedback, assignment)
    noCollectSubmissionsNotification.userLookup = userLookup
    noCollectSubmissionsNotification.recipientUserId = recipient.getUserId
  }

  @Test def collectSubmissions(): Unit = new CollectSubmissionsFixture {
    collectSubmissionsNotification.titleFor(recipient) should be ("IN101: Marking has been stopped for \"demo\"")

    val content = collectSubmissionsNotification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      s"""Marking has been stopped for 1 submission on IN101 - demo.
         |
         |You won't be able to add marks or feedback for this submission until the admin releases them for marking. Any feedback you have entered so far has been saved.
         |""".stripMargin
    )
  }

  @Test def noCollectSubmissions(): Unit = new NoCollectSubmissionsFixture {
    noCollectSubmissionsNotification.titleFor(recipient) should be ("IN101: Marking has been stopped for \"demo\"")

    val content = noCollectSubmissionsNotification.content
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      s"""Marking has been stopped for 1 submission on IN101 - demo.
         |
         |You won't be able to add marks or feedback for this submission until the admin releases them for marking. Any feedback you have entered so far has been saved.
         |""".stripMargin
    )
  }

  @Test def batch(): Unit = new CollectSubmissionsFixture { new NoCollectSubmissionsFixture {
    val batch = Seq(collectSubmissionsNotification, noCollectSubmissionsNotification)

    StopMarkingBatchedNotificationHandler.titleForBatch(batch, recipient) should be ("Marking has been stopped for 2 assignments")

    val content = StopMarkingBatchedNotificationHandler.contentForBatch(batch)

    // These aren't really the same assignment so they aren't grouped
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be(
      s"""Marking has been stopped for the following assignments:
         |
         |- IN101 - demo (1 submission)
         |- IN101 - demo (1 submission)
         |
         |You won't be able to add marks or feedback for these submissions until the admin releases them for marking. Any feedback you have entered so far has been saved.
         |""".stripMargin)
  }}

}
