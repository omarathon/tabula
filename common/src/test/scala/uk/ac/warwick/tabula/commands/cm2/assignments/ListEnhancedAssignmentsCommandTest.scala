package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands.cm2.assignments.ListEnhancedAssignmentsCommand.BasicAssignmentInfo
import uk.ac.warwick.tabula.data.model.markingworkflow.SingleMarkerWorkflow
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}

import scala.jdk.CollectionConverters._

class ListEnhancedAssignmentsCommandTest extends TestBase with Mockito {

  @Test
  def noMarkersFilter(): Unit = {
    val department = Fixtures.department("in")
    val module = Fixtures.module("in101")
    module.adminDepartment = department

    val assignment = Fixtures.assignment("Test assignment")
    assignment.id = "assignmentId"
    assignment.module = module
    assignment.cm2MarkingWorkflow = null // No workflow
    assignment.feedbackService = smartMock[FeedbackService]
    assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any =>
      assignment.feedbacks.asScala.toSeq
    }

    val info = BasicAssignmentInfo(assignment)

    AssignmentInfoFilters.Status.NoMarkers(info) should be (false)

    val userLookup = new MockUserLookup
    val marker = Fixtures.user("1234567")
    userLookup.registerUserObjects(marker)

    val workflow = SingleMarkerWorkflow("Test", department, Seq(marker))
    assignment.cm2MarkingWorkflow = workflow

    // Still false, until we have some students
    AssignmentInfoFilters.Status.NoMarkers(info) should be (false)

    val feedback1 = Fixtures.assignmentFeedback("0000001")
    val feedback2 = Fixtures.assignmentFeedback("0000002")

    assignment.feedbacks.add(feedback1)
    assignment.feedbacks.add(feedback2)

    AssignmentInfoFilters.Status.NoMarkers(info) should be (true)

    val mf1 = Fixtures.markerFeedback(feedback1)
    mf1.userLookup = userLookup
    feedback1.markerFeedback.add(mf1)

    AssignmentInfoFilters.Status.NoMarkers(info) should be (true)

    mf1.marker = marker

    AssignmentInfoFilters.Status.NoMarkers(info) should be (false)
  }

}

