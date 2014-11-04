package uk.ac.warwick.tabula.data.model.notifications.groups

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class ReleaseSmallGroupSetsNotificationTest extends TestBase {

	val module1 = Fixtures.module("cs118")
	val module2 = Fixtures.module("cs119")
	val module3 = Fixtures.module("cs120")

	val set1 = Fixtures.smallGroupSet("set 1")
	set1.format = SmallGroupFormat.Seminar
	set1.module = module1

	val set2 = Fixtures.smallGroupSet("set 2")
	set2.format = SmallGroupFormat.Seminar
	set2.module = module1

	val set3 = Fixtures.smallGroupSet("set 3")
	set3.format = SmallGroupFormat.Seminar
	set3.module = module1

	val group1 = Fixtures.smallGroup("group 1")
	group1.groupSet = set1

	val group2 = Fixtures.smallGroup("group 2")
	group2.groupSet = set2

	val group3 = Fixtures.smallGroup("group 3")
	group3.groupSet = set3

	@Test def title() = withUser("cuscav", "0672089") {
		 val notification = Notification.init(new ReleaseSmallGroupSetsNotification, currentUser.apparentUser, group1)
		 notification.title should be ("CS118 seminar allocation")
	}

	@Test def titlePlural() = withUser("cuscav", "0672089") {
		val notification = Notification.init(new ReleaseSmallGroupSetsNotification, currentUser.apparentUser, Seq(group1, group2, group3))
		notification.title should be ("CS118 seminar allocations")

		set2.module = module3
		set2.format = SmallGroupFormat.Workshop
		notification.title should be ("CS118 and CS120 seminar and workshop allocations")

		set3.module = module2
		set2.format = SmallGroupFormat.Example
		notification.title should be ("CS118, CS119 and CS120 seminar and example class allocations")
	}

 }
