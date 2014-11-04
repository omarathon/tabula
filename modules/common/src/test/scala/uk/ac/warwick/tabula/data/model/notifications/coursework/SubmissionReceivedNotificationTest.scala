package uk.ac.warwick.tabula.data.model.notifications.coursework

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class SubmissionReceivedNotificationTest extends TestBase {

	@Test def titleOnTime() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) { withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission()
		submission.assignment = assignment
		submission.submittedDate = DateTime.now

		assignment.isLate(submission) should be (false)
		assignment.isAuthorisedLate(submission) should be (false)

		val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: Submission received for \"5,000 word essay\"")
	}}

	@Test def titleOnTimeBeforeExtension() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) { withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission()
		submission.assignment = assignment
		submission.submittedDate = DateTime.now

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
		extension.approve()
		assignment.extensions.add(extension)

		assignment.isLate(submission) should be (false)
		assignment.isAuthorisedLate(submission) should be (false)

		val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: Submission received for \"5,000 word essay\"")
	}}

	@Test def titleLate() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 39, 0, 0)) { withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission()
		submission.assignment = assignment
		submission.submittedDate = DateTime.now

		assignment.isLate(submission) should be (true)
		assignment.isAuthorisedLate(submission) should be (false)

		val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: Late submission received for \"5,000 word essay\"")
	}}

	@Test def titleLateWithinExtension() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 39, 0, 0)) { withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission()
		submission.assignment = assignment
		submission.submittedDate = DateTime.now

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
		extension.approve()
		assignment.extensions.add(extension)

		assignment.isLate(submission) should be (false)
		assignment.isAuthorisedLate(submission) should be (true)

		val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: Authorised late submission received for \"5,000 word essay\"")
	}}

	@Test def titleLateAfterExtension() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) { withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val submission = Fixtures.submission()
		submission.assignment = assignment
		submission.submittedDate = DateTime.now

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
		extension.approve()
		assignment.extensions.add(extension)

		assignment.isLate(submission) should be (true)
		assignment.isAuthorisedLate(submission) should be (false)

		val notification = Notification.init(new SubmissionReceivedNotification, currentUser.apparentUser, submission, assignment)
		notification.title should be ("CS118: Late submission received for \"5,000 word essay\"")
	}}

}
