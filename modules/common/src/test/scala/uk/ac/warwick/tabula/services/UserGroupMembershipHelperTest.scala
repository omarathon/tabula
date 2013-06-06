package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, DayOfWeek, SmallGroupEvent}
import uk.ac.warwick.userlookup.User
import org.springframework.transaction.annotation.Transactional
import org.joda.time.LocalTime

class UserGroupMembershipHelperTest extends AppContextTestBase {

  var user:User = _
  var fakeGroups = Map(
    "cusebr" -> List("unrelated-but-cool-group")
  )

  trait FakeLookups { self: UserGroupMembershipHelper[_] =>
    override def getUser(usercode: String) = user
    override def getWebgroups(usercode: String) = fakeGroups.getOrElse(usercode, Nil)
  }

	@Transactional
	@Test
	def groupsAndEvents() {
		val group = new SmallGroup()
		group.name = "Ron"

		def newEvent(tutors:Seq[String]) = {
			val event = new SmallGroupEvent()
			event.startTime = LocalTime.now()
			event.endTime = LocalTime.now()
			event.day = DayOfWeek.Thursday
			for (tutor <- tutors) event.tutors.addUser(tutor)
			event.group = group
      group.events.add(event)
			event
		}

		val event1 = newEvent(Seq("cusebr"))
		val event2 = newEvent(Seq("cusebr", "cuscav"))
		newEvent(Seq("cuscav"))

    session.save(group)

		user = new User("cusebr")
		user.setUserId("cusebr")
		user.setWarwickId("0123456")

		val eventHelper = new UserGroupMembershipHelper[SmallGroupEvent]("tutors") with FakeLookups
		val events = eventHelper.findBy(user)
		events should have size (2)
		events should contain (event1)
		events should contain (event2)

		// cached
		eventHelper.findBy(user)

		val groupHelper = new UserGroupMembershipHelper[SmallGroup]("events.tutors") with FakeLookups
		groupHelper.findBy(user) should be (Seq(group))


	}
}
