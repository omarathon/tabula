package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._


class OnlineFeedbackCommandTest extends TestBase with Mockito {

	trait Fixture {

		// user1 and user3 deliberately have same uniId, to test members with multiple logins.
		val marker = Fixtures.user(universityId="marker", userId="marker")

		val user1 = Fixtures.user(universityId="user1", userId="user1")
		val user2 = Fixtures.user(universityId="user2", userId="user2")
		val user3 = Fixtures.user(universityId="user1", userId="user3")

		val assignment = new Assignment
		val module = new Module
		assignment.module = module

		val submission1 = new Submission("user1")

		val feedback1 = new AssignmentFeedback
		feedback1.assignment = assignment 
		val feedback2 = Fixtures.assignmentFeedback("user2")
		feedback2.released = true
		feedback2.assignment = assignment

		assignment.submissions.add(submission1)
		assignment.feedbacks.add(feedback2)

		val command = new OnlineFeedbackCommand(module, assignment, new CurrentUser(marker, marker)) with OnlineFeedbackCommandTestSupport

		command.assignmentMembershipService.determineMembershipUsers(assignment) returns Seq(user1, user2, user3)

		command.userLookup.registerUserObjects(user1,user2)

		command.submissionService.getSubmissionByUniId(assignment, "user1") returns Some(submission1)
		command.submissionService.getSubmissionByUniId(assignment, "user2") returns None

		command.feedbackService.getAssignmentFeedbackByUniId(assignment, "user1") returns Some(feedback1)
		command.feedbackService.getAssignmentFeedbackByUniId(assignment, "user2") returns Some(feedback2)

	}


	@Test
	def commandApply() {
		new Fixture {
			val feedbackGraphs = command.applyInternal()
			there was one(command.feedbackService).getAssignmentFeedbackByUniId(assignment, "user1")
			there was one(command.feedbackService).getAssignmentFeedbackByUniId(assignment, "user2")

			there was one(command.submissionService).getSubmissionByUniId(assignment, "user1")
			there was one(command.submissionService).getSubmissionByUniId(assignment, "user2")

			val graph1 = feedbackGraphs(0)
			graph1.student should be(user1)
			graph1.hasSubmission should be {true}
			graph1.hasUncompletedFeedback should be {true}
			graph1.hasPublishedFeedback should be {false}
			graph1.hasCompletedFeedback should be {false}

			val graph2 = feedbackGraphs(1)
			graph2.student should be(user2)
			graph2.hasSubmission should be {false}
			graph2.hasUncompletedFeedback should be {true}
			graph2.hasPublishedFeedback should be {true}
			graph2.hasCompletedFeedback should be {false}

			feedbackGraphs.size should be(2)
		}
	}

}

// Implements the dependencies declared by the command
trait OnlineFeedbackCommandTestSupport extends SubmissionServiceComponent with FeedbackServiceComponent with UserLookupComponent with AssignmentMembershipServiceComponent with Mockito {
	val userLookup = new MockUserLookup
	val submissionService = mock[SubmissionService]
	val feedbackService = mock[FeedbackService]
	var assignmentMembershipService = mock[AssessmentMembershipService]
	def apply(): Seq[StudentFeedbackGraph] = Seq()
}