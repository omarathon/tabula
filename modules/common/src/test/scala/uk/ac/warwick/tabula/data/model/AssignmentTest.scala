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
import org.joda.time.DateTimeConstants
import uk.ac.warwick.tabula.data.model.forms.{FormFieldContext, TextField, FormField, Extension}

// scalastyle:off magic.number
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
		assignment.addDefaultSubmissionFields
		assignment.findField(Assignment.defaultCommentFieldName) should be ('defined)
		assignment.addDefaultFeedbackFields
		assignment.findField(Assignment.defaultFeedbackTextFieldName) should be ('defined)

		val ff = new TextField
		ff.name = "pantStyle"
		ff.value = "brief"
		ff.context = FormFieldContext.Feedback
		assignment.addField(ff)
		assignment.findField("pantStyle") should be ('defined)
	}

	@Test(expected=classOf[IllegalArgumentException])
	def fieldContext {
		val assignment = new Assignment
		val ff = new TextField
		ff.name = "destinedToFail"
		ff.value = "instantly"
		assignment.addField(ff)
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

	@Test def inBetweenDays {
		val assignment = new Assignment
		assignment.openDate = new DateTime(2013, DateTimeConstants.JANUARY, 13, 0, 0, 0, 0)
		assignment.closeDate = new DateTime(2013, DateTimeConstants.JANUARY, 30, 0, 0, 0, 0)
		assignment.openEnded = false

		assignment.isOpened(new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)) should be (false)
		assignment.isOpened(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) should be (true)

		assignment.isClosed(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) should be (false)
		assignment.isClosed(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) should be (true)

		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)) should be (false)
		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) should be (true)
		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) should be (false)

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)) {
			assignment.isOpened should be (false)
			assignment.isClosed should be (false)
			assignment.isBetweenDates() should be (false)
		}

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) {
			assignment.isOpened should be (true)
			assignment.isClosed should be (false)
			assignment.isBetweenDates() should be (true)
		}

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) {
			assignment.isOpened should be (true)
			assignment.isClosed should be (true)
			assignment.isBetweenDates() should be (false)
		}

		assignment.openEnded = true

		assignment.isClosed(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) should be (false)
		assignment.isClosed(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) should be (false)

		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)) should be (false)
		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) should be (true)
		assignment.isBetweenDates(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) should be (true)

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)) {
			assignment.isOpened should be (false)
			assignment.isClosed should be (false)
			assignment.isBetweenDates() should be (false)
		}

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)) {
			assignment.isOpened should be (true)
			assignment.isClosed should be (false)
			assignment.isBetweenDates() should be (true)
		}

		withFakeTime(new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)) {
			assignment.isOpened should be (true)
			assignment.isClosed should be (false)
			assignment.isBetweenDates() should be (true)
		}
	}

	@Test def isLate {
		val assignment = new Assignment
		assignment.openDate = new DateTime(2013, DateTimeConstants.JANUARY, 13, 0, 0, 0, 0)
		assignment.closeDate = new DateTime(2013, DateTimeConstants.JANUARY, 30, 0, 0, 0, 0)
		assignment.openEnded = false

		val submission = new Submission
		submission.userId = "cuscav"

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)

		assignment.isLate(submission) should be (false)
		assignment.isAuthorisedLate(submission) should be (false)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 20, 0, 0, 0, 0)

		assignment.isLate(submission) should be (false)
		assignment.isAuthorisedLate(submission) should be (false)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0)

		assignment.isLate(submission) should be (true)
		assignment.isAuthorisedLate(submission) should be (false)

		val extension = new Extension
		extension.userId = "cuscav"
		extension.approved = true
		extension.expiryDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 12, 0, 0, 0)

		assignment.extensions.add(extension)

		assignment.isLate(submission) should be (false)
		assignment.isAuthorisedLate(submission) should be (true)

		submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 0, 0, 0, 0)

		assignment.isLate(submission) should be (true)
		assignment.isAuthorisedLate(submission) should be (false)

		assignment.openEnded = true

		assignment.isLate(submission) should be (false)
		assignment.isAuthorisedLate(submission) should be (false)
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