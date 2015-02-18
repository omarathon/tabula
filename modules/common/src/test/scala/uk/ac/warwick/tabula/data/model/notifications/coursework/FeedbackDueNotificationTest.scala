package uk.ac.warwick.tabula.data.model.notifications.coursework

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.services.ExtensionService
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.AnonymousUser

class FeedbackDueNotificationTest extends TestBase with Mockito with FreemarkerRendering {

	val freeMarkerConfig = newFreemarkerConfiguration

	@Test def titleDueGeneral() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)
		notification.title should be ("CS118: Feedback for \"5,000 word essay\" is due to be published")
	}

	@Test def outputGeneral() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		assignment.extensionService = smartMock[ExtensionService]

		val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)

		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should be (
			"""Feedback for CS118 Programming for Computer Scientists 5,000 word essay is due in 21 working days on 14 October 2014."""
		)
	}

	// TAB-3303
	@Test def outputGeneralPlural() = withFakeTime(new DateTime(2015, DateTimeConstants.FEBRUARY, 16, 17, 0, 0, 0)) {
		val assignment = Fixtures.assignment("Implementation and Report")
		assignment.module = Fixtures.module("cs404", "Agent Based Systems")
		assignment.closeDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 12, 0, 0, 0)

		assignment.extensionService = smartMock[ExtensionService]

		val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)

		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should be (
			"""Feedback for CS404 Agent Based Systems Implementation and Report is due in 1 working day on 17 February 2015."""
		)
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

	@Test def outputExtension() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension.universityId = "1234567"
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)

		val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)

		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should be (
			"""1234567 had an extension until 17 September 2014 for the assignment CS118 Programming for Computer Scientists 5,000 word essay. Their feedback is due in 22 working days on 15 October 2014."""
		)
	}

	// TAB-3303
	@Test def outputExtensionPlural() = withFakeTime(new DateTime(2015, DateTimeConstants.FEBRUARY, 16, 17, 0, 0, 0)) {
		val assignment = Fixtures.assignment("Implementation and Report")
		assignment.module = Fixtures.module("cs404", "Agent Based Systems")
		assignment.closeDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 12, 0, 0, 0)

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension.universityId = "1234567"
		extension.expiryDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 17, 0, 0, 0)

		val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)

		val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
		notificationContent should be (
			"""1234567 had an extension until 20 January 2015 for the assignment CS404 Agent Based Systems Implementation and Report. Their feedback is due in 1 working day on 17 February 2015."""
		)
	}

	@Test def recipientsExtensionNoSubmissions() = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
		assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

		val extension = Fixtures.extension()
		extension.assignment = assignment
		extension.universityId = "1234567"
		extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)

		val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)
		notification.recipients should be (Seq())
	}

}
