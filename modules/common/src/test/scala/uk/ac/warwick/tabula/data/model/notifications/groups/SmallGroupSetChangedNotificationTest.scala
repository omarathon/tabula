package uk.ac.warwick.tabula.data.model.notifications.groups

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class SmallGroupSetChangedNotificationTest extends TestBase {

	val module = Fixtures.module("cs118")

	val set = Fixtures.smallGroupSet("set 1")
	set.format = SmallGroupFormat.Seminar
	set.module = module

	val group = Fixtures.smallGroup("group 1")
	group.groupSet = set

	@Test def titleStudent() = withUser("cuscav", "0672089") {
		 val notification = Notification.init(new SmallGroupSetChangedStudentNotification, currentUser.apparentUser, group, set)
		 notification.title should be ("CS118: Your seminar allocation has changed")
	}

	@Test def titleTutor() = withUser("cuscav", "0672089") {
		val notification = Notification.init(new SmallGroupSetChangedTutorNotification, currentUser.apparentUser, group, set)
		notification.title should be ("CS118: Your seminar allocation has changed")
	}

}
