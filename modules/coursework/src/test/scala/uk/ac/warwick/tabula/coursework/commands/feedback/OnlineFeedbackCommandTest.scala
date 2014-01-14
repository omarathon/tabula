package uk.ac.warwick.tabula.coursework.commands.feedback

import org.mockito.Mockito._
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{Feedback, Submission, Assignment, Module}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.MembershipItem
import scala.Some
import uk.ac.warwick.tabula.MockUserLookup


class OnlineFeedbackCommandTest extends TestBase with Mockito {



	trait Fixture {
		def fakeUser(id:String) = {
			val newUser = new User(id)
			newUser.setWarwickId(id)
			newUser.setFoundUser(true)
			newUser
		}

		val user1 = fakeUser("user1")
		val user2 = fakeUser("user2")
		val user3 = fakeUser("user3")

		user3.setWarwickId("user1")  //case where two usercodes are linked to the same university id

		val membership1 = new MembershipItem(user1, Some("user1"), Some("user1"), IncludeType, false)
		val membership2 = new MembershipItem(user2, Some("user2"), Some("user2"), IncludeType, false)
		val membership3 = new MembershipItem(user3,Some("user1"),Some("user3"),IncludeType,false)
		val membershipInfo = new AssignmentMembershipInfo(Seq(membership1, membership2, membership3))
		val assignment = new Assignment
		val module = new Module
		val assignmentMembershipService = mock[AssignmentMembershipService]
		when (assignmentMembershipService.determineMembership(assignment.upstreamAssessmentGroups, Option(assignment.members))) thenReturn(membershipInfo)

		assignment.assignmentMembershipService = assignmentMembershipService
		assignment.module = module

		val submission1 = new Submission("user1")

		val feedback1 = new Feedback
		val feedback2 = new Feedback("user2")
		feedback2.released = true

		assignment.submissions.add(submission1)
		assignment.feedbacks.add(feedback2)

		val command = new OnlineFeedbackCommand(module, assignment) with OnlineFeedbackCommandTestSupport

		command.userLookup.registerUserObjects(user1,user2)

		when (command.submissionService.getSubmissionByUniId(assignment, "user1")) thenReturn(Some(submission1))
		when (command.submissionService.getSubmissionByUniId(assignment, "user2")) thenReturn(None)

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

			feedbackGraphs.size should be(2)
		}
	}

}

// Implements the dependencies declared by the command
trait OnlineFeedbackCommandTestSupport extends SubmissionServiceComponent with FeedbackServiceComponent with UserLookupComponent with Mockito {
	val userLookup = new MockUserLookup
	val submissionService = mock[SubmissionService]
	val feedbackService = mock[FeedbackService]
	def apply(): Seq[StudentFeedbackGraph] = Seq()
}