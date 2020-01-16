package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.commands.cm2.feedback.{FeedbackSummaryCommandInternal, FeedbackSummaryState}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services.{FeedbackService, FeedbackServiceComponent}
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback}
import uk.ac.warwick.userlookup.User

class FeedbackSummaryCommandTest extends TestBase with Mockito {

  trait CommandTestSupport extends FeedbackServiceComponent with FeedbackSummaryState {
    val feedbackService: FeedbackService = mock[FeedbackService]
  }

  trait Fixture {
    val assignment = new Assignment
    val student = new User {
      setUserId("student1")
    }
    val feedback = new Feedback

    val command = new FeedbackSummaryCommandInternal(assignment, student) with CommandTestSupport
  }

  @Test
  def retunsNothingWhenNoUserId(): Unit = {
    new Fixture {
      student.setUserId(null)
      command.applyInternal()
      verify(command.feedbackService, times(1)).getFeedbackByUsercode(assignment, null)
    }
  }

  @Test
  def callsFeedbackServiceOnce(): Unit = {
    new Fixture {
      command.applyInternal()
      verify(command.feedbackService, times(1)).getFeedbackByUsercode(assignment, "student1")
    }
  }

}
