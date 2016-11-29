package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services.{FeedbackService, FeedbackServiceComponent}
import uk.ac.warwick.tabula.data.model.{AssignmentFeedback, Assignment, Feedback}
import uk.ac.warwick.userlookup.User
import org.mockito.Mockito._

class FeedbackSummaryCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends FeedbackServiceComponent with FeedbackSummaryCommandState  {
		val feedbackService: FeedbackService = mock[FeedbackService]
	}

	trait Fixture {
		val assignment = new Assignment
		val student = new User { setWarwickId("student1") }
		val feedback = new AssignmentFeedback

		val command = new FeedbackSummaryCommandInternal(assignment, student) with CommandTestSupport
	}

	@Test
	def retunsNothingWhenNoWarwickId() {
		new Fixture {
			student.setWarwickId(null)
			command.applyInternal() should be(None)
		}
	}

	@Test
	def callsFeedbackServiceOnce() {
		new Fixture {
			command.applyInternal()
			verify(command.feedbackService, times(1)).getAssignmentFeedbackByUniId(assignment, "student1")
		}
	}

}
