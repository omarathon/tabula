package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTimeConstants._
import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model.forms.{Extension, FormFieldContext, TextField}
import uk.ac.warwick.tabula.services.{ExtensionService, FeedbackService, SubmissionService}
import uk.ac.warwick.tabula.{Mockito, TestBase}

import scala.jdk.CollectionConverters._

// scalastyle:off magic.number
class AssignmentTest extends TestBase with Mockito {
  @Test def academicYear(): Unit = {

    withFakeTime(dateTime(2011, NOVEMBER)) {
      new Assignment().academicYear.startYear should be(2011)
    }

    withFakeTime(dateTime(2011, MAY)) {
      new Assignment().academicYear.startYear should be(2010)
    }
  }

  @Test def fields(): Unit = {
    val assignment = new Assignment
    assignment.findField(Assignment.defaultCommentFieldName) should not be Symbol("defined")
    assignment.addDefaultSubmissionFields()
    assignment.findField(Assignment.defaultCommentFieldName) should be(Symbol("defined"))
    assignment.addDefaultFeedbackFields()
    assignment.findField(Assignment.defaultFeedbackTextFieldName) should be(Symbol("defined"))

    val ff = new TextField
    ff.name = "pantStyle"
    ff.value = "brief"
    ff.context = FormFieldContext.Feedback
    assignment.addField(ff)
    assignment.findField("pantStyle") should be(Symbol("defined"))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def fieldContext(): Unit = {
    val assignment = new Assignment
    val ff = new TextField
    ff.name = "destinedToFail"
    ff.value = "instantly"
    assignment.addField(ff)
  }

  @Test def unreleasedFeedback(): Unit = {
    val assignment = new Assignment
    assignment.feedbackService = smartMock[FeedbackService]
    assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any => assignment.feedbacks.asScala.toSeq }

    assignment.feedbacks should be(Symbol("empty"))
    assignment.unreleasedFeedback should be(Symbol("empty"))

    val feedback = mockFeedback(assignment)
    assignment.feedbacks add feedback
    assignment.feedbacks.size should be(1)
    assignment.unreleasedFeedback.size should be(1)
    feedback.released = true
    assignment.unreleasedFeedback should be(Symbol("empty"))
  }

  @Test def placeholderFeedback(): Unit = {
    val assignment = new Assignment
    assignment.feedbackService = smartMock[FeedbackService]
    assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any => assignment.feedbacks.asScala.toSeq }

    assignment.fullFeedback should be(Symbol("empty"))

    val feedback = new Feedback
    feedback.assignment = assignment
    assignment.feedbacks add feedback
    assignment.fullFeedback should be(Symbol("empty"))


    feedback.actualMark = Option(41)
    assignment.fullFeedback.size should be(1)
  }

  @Test def openEnded(): Unit = {
    val assignment = new Assignment
    assignment.openEnded = false

    // past assignment should be closed
    assignment.openDate = new DateTime().minusDays(3)
    assignment.closeDate = new DateTime().minusDays(2)
    assignment.isClosed should be (true)

    // Open Gangnam Style was so 2012
    assignment.openEnded = true
    assignment.isClosed should be (false)
  }

  @Test def assignmentCanPublishFeedback(): Unit = {
    val assignment = new Assignment
    assignment.feedbackService = smartMock[FeedbackService]
    assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any => assignment.feedbacks.asScala.toSeq }

    assignment.feedbacks add mockFeedback(assignment)

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

  @Test def inBetweenDays(): Unit = {
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

  @Test def assignmentIsLate(): Unit = {
    val assignment = new Assignment
    assignment.extensionService = smartMock[ExtensionService]
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

    assignment.openDate = new DateTime(2013, DateTimeConstants.JANUARY, 13, 0, 0, 0, 0)
    assignment.closeDate = new DateTime(2013, DateTimeConstants.JANUARY, 30, 0, 0, 0, 0)
    assignment.openEnded = false

    val submission = new Submission
    submission.usercode = "cuscav"

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
    extension.usercode = "cuscav"
    extension.approve()
    extension.expiryDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 12, 0, 0, 0)

    assignment.addExtension(extension)

    assignment.extensionService = smartMock[ExtensionService]
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map("cuscav" -> extension)

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
  def idFormat(i: Int): String = "%07d" format i

  def mockFeedback(assignment: Assignment): Feedback = {
    val f = new Feedback()
    f.assignment = assignment
    // add a mark so this is not treated like a placeholder
    f.actualMark = Some(41)
    f
  }

  @Test def workingDaysLate(): Unit = {
    val assignment = new Assignment
    assignment.extensionService = smartMock[ExtensionService]
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

    assignment.openDate = new DateTime(2013, DateTimeConstants.JANUARY, 13, 0, 0, 0, 0)
    assignment.closeDate = new DateTime(2013, DateTimeConstants.JANUARY, 30, 12, 0, 0, 0) // Wednesday, 12pm
    assignment.openEnded = false

    val submission = new Submission
    submission.usercode = "cuscav"

    submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)
    assignment.workingDaysLate(submission) should be(0)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 30, 13, 0, 0, 0) // Wednesday, 1pm
    assignment.workingDaysLate(submission) should be(1)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0) // Thursday (morning), midnight
    assignment.workingDaysLate(submission) should be(1)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 14, 0, 0, 0) // Thursday 2pm
    assignment.workingDaysLate(submission) should be(2)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 11, 0, 0, 0) // Saturday 11am
    assignment.workingDaysLate(submission) should be(3)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 14, 0, 0, 0) // Saturday 2pm
    assignment.workingDaysLate(submission) should be(3)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 14, 0, 0, 0) // Sunday 2pm
    assignment.workingDaysLate(submission) should be(3)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 11, 0, 0, 0) // Monday 11am
    assignment.workingDaysLate(submission) should be(3)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 23, 0, 0, 0) // Monday 11pm
    assignment.workingDaysLate(submission) should be(4)

    // Extended until 12pm Friday
    val extension = new Extension
    extension.usercode = "cuscav"
    extension.approve()
    extension.expiryDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 1, 12, 0, 0, 0)

    assignment.addExtension(extension)

    assignment.extensionService = smartMock[ExtensionService]
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map("cuscav" -> extension)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 10, 0, 0, 0, 0)
    assignment.workingDaysLate(submission) should be(0)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 30, 13, 0, 0, 0) // Wednesday, 1pm
    assignment.workingDaysLate(submission) should be(0)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 0, 0, 0, 0) // Thursday (morning), midnight
    assignment.workingDaysLate(submission) should be(0)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.JANUARY, 31, 14, 0, 0, 0) // Thursday 2pm
    assignment.workingDaysLate(submission) should be(0)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 11, 0, 0, 0) // Saturday 11am
    assignment.workingDaysLate(submission) should be(1)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 2, 14, 0, 0, 0) // Saturday 2pm
    assignment.workingDaysLate(submission) should be(1)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 3, 14, 0, 0, 0) // Sunday 2pm
    assignment.workingDaysLate(submission) should be(1)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 11, 0, 0, 0) // Monday 11am
    assignment.workingDaysLate(submission) should be(1)

    submission.submittedDate = new DateTime(2013, DateTimeConstants.FEBRUARY, 4, 23, 0, 0, 0) // Monday 11pm
    assignment.workingDaysLate(submission) should be(2)


    val assignment2 = new Assignment
    assignment2.extensionService = smartMock[ExtensionService]
    assignment2.extensionService.getApprovedExtensionsByUserId(assignment2) returns Map.empty

    assignment2.openDate = new DateTime(2016, DateTimeConstants.SEPTEMBER, 13, 23, 59, 59, 0)
    assignment2.closeDate = new DateTime(2017, DateTimeConstants.SEPTEMBER, 13, 23, 59, 59, 0)
    assignment2.openEnded = false

    val assignment3 = new Assignment
    assignment3.extensionService = smartMock[ExtensionService]
    assignment3.extensionService.getApprovedExtensionsByUserId(assignment3) returns Map.empty

    assignment3.openDate = new DateTime(2016, DateTimeConstants.SEPTEMBER, 14, 0, 0, 0, 0)
    assignment3.closeDate = new DateTime(2017, DateTimeConstants.SEPTEMBER, 14, 0, 0, 0, 0)
    assignment3.openEnded = false

    val s = new Submission
    s.usercode = "u1234567"

    s.submittedDate = new DateTime(2017, 9, 13, 23, 56, 51, 0)
    assignment2.workingDaysLate(s) should be(0)
    assignment3.workingDaysLate(s) should be(0)

    s.submittedDate = new DateTime(2017, 9, 14, 23, 56, 51, 0)
    assignment2.workingDaysLate(s) should be(1)
    assignment3.workingDaysLate(s) should be(1)

    s.submittedDate = new DateTime(2017, 9, 15, 23, 56, 51, 0)
    assignment2.workingDaysLate(s) should be(2)
    assignment3.workingDaysLate(s) should be(2)

    s.submittedDate = new DateTime(2017, 9, 16, 0, 0, 0, 0)
    assignment2.workingDaysLate(s) should be(3) // ignoring the time spent on Sat this is 2 days and 1 second late (round up to 3)
    assignment3.workingDaysLate(s) should be(2)

    s.submittedDate = new DateTime(2017, 9, 17, 23, 56, 51, 0)
    assignment2.workingDaysLate(s) should be(3) // ignoring the time spent on Sat and Sun this is 2 days and 1 second late (round up to 3)
    assignment3.workingDaysLate(s) should be(2)

  }

  @Test def testHasOutstandingFeedback(): Unit = {
    val assignment = new Assignment
    assignment.dissertation = false
    assignment.openDate = new DateTime(2015, DateTimeConstants.APRIL, 1, 12, 0, 0, 0)
    assignment.closeDate = assignment.openDate.plusWeeks(2)

    assignment.submissionService = smartMock[SubmissionService]
    assignment.feedbackService = smartMock[FeedbackService]
    assignment.extensionService = smartMock[ExtensionService]

    assignment.extensionService.hasExtensions(assignment) returns false
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

    assignment.submissionService.getSubmissionsByAssignment(assignment) returns Nil

    assignment.hasOutstandingFeedback should be(false)

    reset(assignment.submissionService)

    val submission1 = new Submission
    submission1.usercode = "0000001"
    submission1.assignment = assignment

    val submission2 = new Submission
    submission2.usercode = "0000002"
    submission2.assignment = assignment

    assignment.submissionService.getSubmissionsByAssignment(assignment) returns Seq(submission1, submission2)
    assignment.openEnded = true // so submissions don't need a feedback deadline

    assignment.hasOutstandingFeedback should be(false)

    assignment.openEnded = false

    assignment.feedbackService.getFeedbackByUsercode(assignment, "0000001") returns None
    assignment.feedbackService.getFeedbackByUsercode(assignment, "0000002") returns None

    assignment.hasOutstandingFeedback should be(true)

    reset(assignment.feedbackService)

    // Publish feedback for student 1, add feedback for student 2 but don't publish it
    val feedback1 = new Feedback
    feedback1.released = true

    val feedback2 = new Feedback
    feedback2.released = false

    assignment.feedbackService.getFeedbackByUsercode(assignment, "0000001") returns Some(feedback1)
    assignment.feedbackService.getFeedbackByUsercode(assignment, "0000002") returns Some(feedback2)

    assignment.hasOutstandingFeedback should be(true)

    // Once feedback2 is released, there's no longer outstanding feedback
    feedback2.released = true

    assignment.hasOutstandingFeedback should be(false)
  }

}
