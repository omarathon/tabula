package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.data.FeedbackForSitsDaoImpl
import uk.ac.warwick.tabula.data.model.FeedbackForSitsStatus.UploadNotAttempted
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.{Mockito, Fixtures, TestBase}
import uk.ac.warwick.userlookup.User

class QueueFeedbackForSitsCommandTest extends TestBase with Mockito {
	@Test def testApply = withUser("0070790", "cusdx") {

		val user = Fixtures.user()
		val feedback = Fixtures.feedback(user.getWarwickId)
		feedback.actualMark = Some(63)
		feedback.actualGrade = Some("A")

		val assignment = Fixtures.assignment("test assignment")
		assignment.feedbacks.add(feedback)
		feedback.assignment = assignment

		val feedbackService = smartMock[FeedbackService]
		feedbackService.getUsersForFeedback(assignment) returns (Seq[(String, User)]((user.getWarwickId, user)))

		// make like this feedback isn't queued yet
		val feedbackForSitsDao = smartMock[FeedbackForSitsDaoImpl]
		feedbackForSitsDao.getByFeedback(feedback) returns (None)

		// set up the command
		val cmd = new QueueFeedbackForSitsCommand(assignment, currentUser)
		cmd.feedbackService = feedbackService
		cmd.feedbackForSitsDao = feedbackForSitsDao

		// call apply and look to see if the expected FeedbackForSits option is returned
		val queuedFeedbacks = cmd.applyInternal()
		queuedFeedbacks.size should be (1)
		queuedFeedbacks.head.status should be (UploadNotAttempted)
		queuedFeedbacks.head.feedback.actualMark should be (Some(63))
		queuedFeedbacks.head.feedback.actualGrade should be (Some("A"))
	}

}
