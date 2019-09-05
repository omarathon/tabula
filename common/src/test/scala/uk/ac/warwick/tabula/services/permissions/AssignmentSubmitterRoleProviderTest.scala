package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{UpstreamAssessmentGroupInfo, UserGroup}
import uk.ac.warwick.tabula.roles.AssignmentSubmitter
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, FeedbackService}

import scala.collection.JavaConverters._

class AssignmentSubmitterRoleProviderTest extends TestBase with Mockito {

  val provider = new AssignmentSubmitterRoleProvider
  val assignmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]

  @Test def unrestrictedAssignment = withUser("cuscav") {
    val assignment = Fixtures.assignment("my assignment")
    assignment.module = Fixtures.module("in101")
    assignment.module.adminDepartment = Fixtures.department("in")

    assignment.restrictSubmissions = false

    provider.getRolesFor(currentUser, assignment) should be(Seq(AssignmentSubmitter(assignment)))
  }

  @Test def canSubmit = withUser("cuscav") {
    val assignment = Fixtures.assignment("my assignment")
    assignment.module = Fixtures.module("in101")
    assignment.module.adminDepartment = Fixtures.department("in")
    assignment.feedbackService = smartMock[FeedbackService]
    assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _ => assignment.feedbacks.asScala }

    assignment.assessmentMembershipService = assignmentMembershipService
    assignment.restrictSubmissions = true

    assignmentMembershipService.isStudentCurrentMember(
      isEq(currentUser.apparentUser),
      isA[Seq[UpstreamAssessmentGroupInfo]],
      isA[Option[UserGroup]],
      any[Boolean]) returns (true)

    provider.getRolesFor(currentUser, assignment) should be(Seq(AssignmentSubmitter(assignment)))
  }

  @Test def cannotSubmit = withUser("cuscav") {
    val assignment = Fixtures.assignment("my assignment")
    assignment.assessmentMembershipService = assignmentMembershipService
    assignment.restrictSubmissions = true
    assignment.feedbackService = smartMock[FeedbackService]
    assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _ => assignment.feedbacks.asScala }

    assignmentMembershipService.isStudentCurrentMember(
      isEq(currentUser.apparentUser),
      isA[Seq[UpstreamAssessmentGroupInfo]],
      isA[Option[UserGroup]],
      any[Boolean]) returns (false)

    provider.getRolesFor(currentUser, assignment) should be(Seq())
  }

  @Test def handlesDefault = withUser("cuscav") {
    provider.getRolesFor(currentUser, Fixtures.department("in", "IT Services")) should be(Seq())
  }

}