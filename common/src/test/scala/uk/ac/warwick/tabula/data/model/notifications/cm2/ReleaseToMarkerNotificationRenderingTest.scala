package uk.ac.warwick.tabula.data.model.notifications.cm2

import java.io.{ByteArrayOutputStream, OutputStreamWriter, StringWriter}

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import org.junit.Before
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{DblFinalMarker, DblFirstMarker, DblSecondMarker}
import uk.ac.warwick.tabula.services.ExtensionService
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ReleaseToMarkerNotificationRenderingTest extends TestBase with Mockito {

  var dept: Department = _

  var assignment: Assignment = _

  var feedback: Feedback = _
  val marker1: User = Fixtures.user("9999991", "9999991")
  val marker2: User = Fixtures.user("9999992", "9999992")

  var markerFeedback: MarkerFeedback = _


  @Before
  def prepare(): Unit = {
    dept = Fixtures.department("in")

    assignment = Fixtures.assignment("demo")
    assignment.closeDate = new LocalDate(2018, DateTimeConstants.SEPTEMBER, 11).toDateTime(Assignment.defaultCloseTime)
    assignment.extensionService = smartMock[ExtensionService]
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

    feedback = Fixtures.assignmentFeedback("9999991", "9999992")
    feedback.assignment = assignment
    markerFeedback = Fixtures.markerFeedback(feedback)
  }

  @Test
  def rendersWhenNotCollectSubmissions(): Unit = {
    assignment.collectSubmissions = false
    val output = new ByteArrayOutputStream
    val writer = new OutputStreamWriter(output)
    val configuration = newFreemarkerConfiguration()
    val template = configuration.getTemplate(ReleaseToMarkerNotification.templateLocation)
    template.process(ReleaseToMarkerNotification.renderNoCollectingSubmissions(
      assignment = assignment,
      feedbacksCount = 10,
      workflowVerb = "The_Verb"
    ).model, writer)
    writer.flush()
    val renderedResult = output.toString
    renderedResult.trim should be(
      """
        |10 students are allocated to you for marking.
        |- This assignment does not require students to submit work to Tabula
      """.stripMargin.trim
    )
  }

  @Test
  def renderWhenCollectingSubmissionsWithManualRelease(): Unit = {
    assignment.collectSubmissions = true
    val output = new ByteArrayOutputStream
    val writer = new OutputStreamWriter(output)
    val configuration = newFreemarkerConfiguration()
    val template = configuration.getTemplate(ReleaseToMarkerNotification.templateLocation)
    template.process(ReleaseToMarkerNotification.renderCollectSubmissions(
      assignment = assignment,
      allocatedStudentsCount = 6,
      studentsAtStagesCount = Seq(
        StudentAtStagesCount(DblFirstMarker.description, 2),
        StudentAtStagesCount(DblSecondMarker.description, 1),
        StudentAtStagesCount(DblFinalMarker.description, 3)
      ),
      feedbacksCount = 12,
      submissionsCount = 13,
      noSubmissionsWithExtensionCount = 4,
      noSubmissionsWithoutExtensionCount = 3,
      workflowVerb = "the_verb"
    ).model, writer)
    writer.flush()
    val renderedResult = output.toString
    renderedResult.trim should be(
      """
        |6 students are allocated to you for marking.
        |- First marker: 2 students
        |- Second marker: 1 student
        |- Final marker: 3 students
        |
        |12 students allocated to you have been released for marking:
        |- 13 students have submitted work that can be marked
        |- 4 students have not submitted but they have an extension
        |- 3 students have not submitted work and have not yet requested an extension
        |
        |Student feedback is due on 9 October 2018.
      """.stripMargin.trim)
  }

  @Test
  def renderWhenCollectingSubmissionsWithAutoRelease(): Unit = {
    assignment.collectSubmissions = true
    assignment.automaticallyReleaseToMarkers_=(true)
    val output = new ByteArrayOutputStream
    val writer = new OutputStreamWriter(output)
    val configuration = newFreemarkerConfiguration()
    val template = configuration.getTemplate(ReleaseToMarkerNotification.templateLocation)
    template.process(ReleaseToMarkerNotification.renderCollectSubmissions(
      assignment = assignment,
      allocatedStudentsCount = 6,
      studentsAtStagesCount = Seq(
        StudentAtStagesCount(DblFirstMarker.description, 2),
        StudentAtStagesCount(DblSecondMarker.description, 1),
        StudentAtStagesCount(DblFinalMarker.description, 3)
      ),
      feedbacksCount = 12,
      submissionsCount = 13,
      noSubmissionsWithExtensionCount = 4,
      noSubmissionsWithoutExtensionCount = 3,
      workflowVerb = "the_verb"
    ).model, writer)
    writer.flush()
    val renderedResult = output.toString
    renderedResult.trim should be(
      """
        |6 students are allocated to you for marking.
        |- First marker: 2 students
        |- Second marker: 1 student
        |- Final marker: 3 students
        |
        |12 students allocated to you have been released for marking as the assignment has been set to automatically release when the end date and time have been reached:
        |- 13 students have submitted work that can be marked
        |- 4 students have not submitted but they have an extension
        |- 3 students have not submitted work and have not yet requested an extension
        |
        |Student feedback is due on 9 October 2018.
      """.stripMargin.trim)
  }

  @Test
  def correctPlural(): Unit = {
    assignment.collectSubmissions = true
    assignment.automaticallyReleaseToMarkers_=(true)
    val output = new ByteArrayOutputStream
    val writer = new OutputStreamWriter(output)
    val configuration = newFreemarkerConfiguration()
    val template = configuration.getTemplate(ReleaseToMarkerNotification.templateLocation)
    template.process(ReleaseToMarkerNotification.renderCollectSubmissions(
      assignment = assignment,
      allocatedStudentsCount = 6,
      studentsAtStagesCount = Seq(
        StudentAtStagesCount(DblFirstMarker.description, 2),
        StudentAtStagesCount(DblSecondMarker.description, 1),
        StudentAtStagesCount(DblFinalMarker.description, 3)
      ),
      feedbacksCount = 12,
      submissionsCount = 1,
      noSubmissionsWithExtensionCount = 1,
      noSubmissionsWithoutExtensionCount = 1,
      workflowVerb = "the_verb"
    ).model, writer)
    writer.flush()
    val renderedResult = output.toString
    renderedResult.trim should be(
      """
        |6 students are allocated to you for marking.
        |- First marker: 2 students
        |- Second marker: 1 student
        |- Final marker: 3 students
        |
        |12 students allocated to you have been released for marking as the assignment has been set to automatically release when the end date and time have been reached:
        |- 1 student has submitted work that can be marked
        |- 1 student has not submitted but they have an extension
        |- 1 student has not submitted work and has not yet requested an extension
        |
        |Student feedback is due on 9 October 2018.
      """.stripMargin.trim)
  }

  @Test
  def renderWhenCollectingSubmissionsWithAutoReleaseWithZeroAllocation(): Unit = {
    assignment.collectSubmissions = true
    assignment.automaticallyReleaseToMarkers_=(true)
    val output = new ByteArrayOutputStream
    val writer = new OutputStreamWriter(output)
    val configuration = newFreemarkerConfiguration()
    val template = configuration.getTemplate(ReleaseToMarkerNotification.templateLocation)
    template.process(ReleaseToMarkerNotification.renderCollectSubmissions(
      assignment = assignment,
      allocatedStudentsCount = 6,
      studentsAtStagesCount = Seq(
        StudentAtStagesCount(DblFirstMarker.description, 2),
        StudentAtStagesCount(DblSecondMarker.description, 1),
        StudentAtStagesCount(DblFinalMarker.description, 3)
      ),
      feedbacksCount = 0,
      submissionsCount = 123123,
      noSubmissionsWithExtensionCount = 4,
      noSubmissionsWithoutExtensionCount = 3,
      workflowVerb = "the_verb"
    ).model, writer)
    writer.flush()
    val renderedResult = output.toString
    renderedResult.trim should be(
      """
        |6 students are allocated to you for marking.
        |- First marker: 2 students
        |- Second marker: 1 student
        |- Final marker: 3 students
        |
        |No students allocated to you have been released for marking as the assignment has been set to automatically release when the end date and time have been reached but no students have yet submitted.
        |
        |Please check the assignment regularly as students with extensions may submit at any time.
        |
        |Student feedback is due on 9 October 2018.
      """.stripMargin.trim)
  }
}
