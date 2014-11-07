package uk.ac.warwick.tabula.data.model.notifications.coursework

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class RequestAssignmentAccessNotificationTest extends TestBase {

	@Test def title() = withUser("cuscav", "0672089") {
		val assignment = Fixtures.assignment("5,000 word essay")
		assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")

		val notification = Notification.init(new RequestAssignmentAccessNotification, currentUser.apparentUser, assignment)
		notification.title should be ("CS118: Access requested for \"5,000 word essay\"")
	}

}
