package uk.ac.warwick.courses.data.model
import uk.ac.warwick.courses.TestBase
import org.junit.Test
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._

class AssignmentTest extends TestBase {
	@Test def academicYear {
		
		withFakeTime(dateTime(2011,NOVEMBER)) {
			new Assignment().academicYear.startYear should be (2011)
		}
		
		withFakeTime(dateTime(2011,MAY)) { 
			new Assignment().academicYear.startYear should be (2010)
		}
	}
	
	@Test def fields {
		val assignment = new Assignment
		assignment.findField(Assignment.defaultCommentFieldName) should not be ('defined)
		assignment.addDefaultFields
		assignment.findField(Assignment.defaultCommentFieldName) should be ('defined)
	}
	
	@Test def unreleasedFeedback {
		val assignment = new Assignment
		assignment.feedbacks should be ('empty)
		assignment.unreleasedFeedback should be ('empty)
		
		val feedback = new Feedback
		assignment.feedbacks add feedback
		assignment.feedbacks.size should be (1)
		assignment.unreleasedFeedback.size should be (1)		
		feedback.released = true
		assignment.unreleasedFeedback should be ('empty)
	}
	
}