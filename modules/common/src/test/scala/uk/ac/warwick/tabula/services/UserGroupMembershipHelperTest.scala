package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, DayOfWeek, SmallGroupEvent}
import uk.ac.warwick.userlookup.User
import org.springframework.transaction.annotation.Transactional
import org.joda.time.LocalTime

class UserGroupMembershipHelperTest extends AppContextTestBase {

	@Transactional
	@Test
	def groupsAndEvents() {
		val group = new SmallGroup()
		group.name = "Ron"
		session.save(group)

		def newEvent(tutors:Seq[String]) = {
			val event = new SmallGroupEvent()
			event.startTime = LocalTime.now()
			event.endTime = LocalTime.now()
			event.day = DayOfWeek.Thursday
			for (tutor <- tutors) event.tutors.addUser(tutor)
			event.group = group
			session.save(event)
			event
		}

		val event1 = newEvent(Seq("cusebr"))
		val event2 = newEvent(Seq("cusebr", "cuscav"))
		newEvent(Seq("cuscav"))

		val user = new User("cusebr")
		user.setUserId("cusebr")
		user.setWarwickId("0123456")

		val eventHelper = new UserGroupMembershipHelper[SmallGroupEvent]("tutors") {
			override def getUser(usercode: String) = user
		}
		val events = eventHelper.findBy(user)
		events should have size (2)
		events should contain (event1)
		events should contain (event2)

		// cached
		eventHelper.findBy(user)

		val groupHelper = new UserGroupMembershipHelper[SmallGroup]("events.tutors") {
			override def getUser(usercode: String) = user
		}
		groupHelper.findBy(user) should be (Seq(group))


	}
}
