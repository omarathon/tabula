package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services.{FeedbackService, FeedbackServiceComponent}
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback}
import uk.ac.warwick.userlookup.User
import org.mockito.Mockito._

class FeedbackSummaryCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends FeedbackServiceComponent with FeedbackSummaryCommandState  {
		val feedbackService = mock[FeedbackService]
	}

	trait Fixture {
		val assignment = new Assignment
		val student = new User { setWarwickId("student1") }
		val feedback = new Feedback

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
			verify(command.feedbackService, times(1)).getFeedbackByUniId(assignment, "student1")
		}
	}

}
