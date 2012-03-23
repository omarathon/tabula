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
	
	@Test def submissionsReport {
		val assignment = new Assignment
		assignment.submissionsReport should not be ('hasProblems) // not be has problems?
		
		for (i <- 1 to 10) // 0000001 .. 0000010 
			assignment.feedbacks add new Feedback(universityId = idFormat(i))
		
		for (i <- 8 to 20) // 0000008 .. 0000020
			assignment.submissions add new Submission(universityId = idFormat(i))
		
		// only 0000008 .. 0000010 are common to both lists
		val report = assignment.submissionsReport
		report should be ('hasProblems) // be has problems.
		report.feedbackOnly.toSeq.sorted should be ((1 to 7) map idFormat)
		report.submissionOnly.toSeq.sorted should be ((11 to 20) map idFormat)
		
	}
	
	/** Zero-pad integer to a 7 digit string */
	def idFormat(i:Int) = "%07d" format i
}