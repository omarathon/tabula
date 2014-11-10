package uk.ac.warwick.tabula.data.model.notifications.coursework

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.AnonymousUser

class FeedbackDueNotificationTest extends TestBase with Mockito {

	@Test def titleDueGeneral() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)
		notification.title should be ("CS118: Feedback for \"5,000 word essay\" is due to be published")
	}

	@Test def titleDueExtension() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension.universityId = "1234567"
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)

		val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)
		notification.title should be ("CS118: Feedback for 1234567 for \"5,000 word essay\" is due to be published")
	}

}
