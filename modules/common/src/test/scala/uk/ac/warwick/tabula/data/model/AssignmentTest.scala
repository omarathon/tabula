package uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.junit.Test

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
		
		val feedback = mockFeedback
		assignment.feedbacks add feedback
		assignment.feedbacks.size should be (1)
		assignment.unreleasedFeedback.size should be (1)
		feedback.released = true
		assignment.unreleasedFeedback should be ('empty)
	}

	@Test def placeholderFeedback {
		val assignment = new Assignment
		assignment.fullFeedback should be ('empty)

		val feedback = new Feedback
		assignment.feedbacks add feedback
		assignment.fullFeedback should be ('empty)

		feedback.actualMark = Some(41)
		assignment.fullFeedback.size should be (1)
	}
	
	@Test def submissionsReport {
		val assignment = new Assignment
		assignment.submissionsReport should not be ('hasProblems) // not be has problems?
		
		for (i <- 1 to 10) {// 0000001 .. 0000010
			val feedback = new Feedback(universityId = idFormat(i))
			feedback.actualMark = Some(i*10)
			assignment.feedbacks add feedback
		}
		
		for (i <- 8 to 20) // 0000008 .. 0000020
			assignment.submissions add new Submission(universityId = idFormat(i))
		
		// only 0000008 .. 0000010 are common to both lists
		val report = assignment.submissionsReport
		assignment.collectSubmissions = false
		report should not be ('hasProblems) // be has problems.
		assignment.collectSubmissions = true
		report should be ('hasProblems) // be has problems.
		report.feedbackOnly.toSeq.sorted should be ((1 to 7) map idFormat)
		report.submissionOnly.toSeq.sorted should be ((11 to 20) map idFormat)		
	}
	
	@Test def openEnded {
		val assignment = new Assignment
		// assign to a val so that 'should' can cope with JBoolean
		val isOpenEnded: Boolean = assignment.openEnded
		// test default
		isOpenEnded should be (false)
		
		// past assignment should be closed
		assignment.openDate = new DateTime().minusDays(3)
		assignment.closeDate = new DateTime().minusDays(2)
		assignment.isClosed should be (true)
		
		// Open Gangnam Style
		assignment.openEnded = true
		assignment.isClosed should be (false)
	}
	
	@Test def canPublishFeedback {
		val assignment = new Assignment
		assignment.feedbacks add mockFeedback

		assignment.openDate = new DateTime().minusDays(3)
		assignment.closeDate = new DateTime().plusDays(10)
		assignment.openEnded = false
		// can't publish until closed
		assignment.canPublishFeedback should be (false)
		
		// unless open-ended
		assignment.openEnded = true
		assignment.canPublishFeedback should be (true)
		
		// now it's closed
		assignment.openEnded = false
		assignment.closeDate = new DateTime().minusDays(1)
		assignment.canPublishFeedback should be (true)
	}
	
	/** Zero-pad integer to a 7 digit string */
	def idFormat(i:Int) = "%07d" format i

	def mockFeedback:Feedback = {
		val f = new Feedback()
		// add a mark so this is not treated like a placeholder
		f.actualMark = Some(41)
		f
	}
}