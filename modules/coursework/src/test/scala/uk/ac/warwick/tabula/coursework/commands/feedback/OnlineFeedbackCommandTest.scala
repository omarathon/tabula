package uk.ac.warwick.tabula.coursework.commands.feedback

import org.mockito.Mockito._
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{Feedback, Submission, Assignment, Module}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.MembershipItem
import scala.Some

class OnlineFeedbackCommandTest extends TestBase with Mockito {



	trait Fixture {
		def fakeUser(id:String) = {
			val newUser = new User(id)
			newUser.setWarwickId(id)
			newUser
		}

		val user1 = fakeUser("user1")
		val user2 = fakeUser("user2")

		val membership1 = new MembershipItem(user1, Some("user1"), Some("user1"), IncludeType, false)
		val membership2 = new MembershipItem(user2, Some("user2"), Some("user2"), IncludeType, false)
		val membershipInfo = new AssignmentMembershipInfo(Seq(membership1, membership2))
		val assignment = new Assignment
		val module = new Module
		val assignmentMembershipService = mock[AssignmentMembershipService]
		when (assignmentMembershipService.determineMembership(assignment.upstreamAssessmentGroups, Option(assignment.members))) thenReturn(membershipInfo)

		assignment.assignmentMembershipService = assignmentMembershipService
		assignment.module = module
		val command = new OnlineFeedbackCommand(module, assignment) with OnlineFeedbackCommandTestSupport

		val submission1 = new Submission
		val submission2 = new Submission
		when (command.submissionService.getSubmissionByUniId(assignment, "user1")) thenReturn(Some(submission1))
		when (command.submissionService.getSubmissionByUniId(assignment, "user2")) thenReturn(None)

		val feedback1 = new Feedback
		val feedback2 = new Feedback
		feedback2.released = true
		when (command.feedbackService.getFeedbackByUniId(assignment, "user1")) thenReturn(Some(feedback1))
		when (command.feedbackService.getFeedbackByUniId(assignment, "user2")) thenReturn(Some(feedback2))
	}


	@Test
	def commandApply() {
		new Fixture {
			val feedbackGraphs = command.applyInternal()
			there was one(command.feedbackService).getFeedbackByUniId(assignment, "user1")
			there was one(command.feedbackService).getFeedbackByUniId(assignment, "user2")

			there was one(command.submissionService).getSubmissionByUniId(assignment, "user1")
			there was one(command.submissionService).getSubmissionByUniId(assignment, "user2")

			val graph1 = feedbackGraphs(0)
			graph1.student should be(user1)
			graph1.hasSubmission should be(true)
			graph1.hasFeedback should be(true)
			graph1.hasPublishedFeedback should be(false)
			graph1.hasCompletedFeedback should be(false)

			val graph2 = feedbackGraphs(1)
			graph2.student should be(user2)
			graph2.hasSubmission should be(false)
			graph2.hasFeedback should be(true)
			graph2.hasPublishedFeedback should be(true)
			graph2.hasCompletedFeedback should be(false)
		}
	}
}

// Implements the dependencies declared by the command
trait OnlineFeedbackCommandTestSupport extends SubmissionServiceComponent with FeedbackServiceComponent with Mockito {
	val submissionService = mock[SubmissionService]
	val feedbackService = mock[FeedbackService]
	def apply(): Seq[StudentFeedbackGraph] = Seq()
}