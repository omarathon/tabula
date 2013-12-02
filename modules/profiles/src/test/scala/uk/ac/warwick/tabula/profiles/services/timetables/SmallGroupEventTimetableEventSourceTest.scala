package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services.{SmallGroupService, UserLookupService, SmallGroupServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.data.model.{Module, StudentMember}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.JavaImports.JArrayList
import org.joda.time.LocalTime
import uk.ac.warwick.tabula.Fixtures

class SmallGroupEventTimetableEventSourceTest extends TestBase with Mockito{

	val mockSmallGroupService = mock[SmallGroupService]
	val mockUserLookup = mock[UserLookupService]

	val eventSource = new SmallGroupEventTimetableEventSourceComponentImpl with UserLookupComponent with SmallGroupServiceComponent {
		def smallGroupService: SmallGroupService = mockSmallGroupService
		def userLookup: UserLookupService = mockUserLookup
	}.studentGroupEventSource

	val student = Fixtures.student(universityId="0000001", userId="studentUserId")
	val studentUser = new User
	mockUserLookup.getUserByUserId(any[String]) returns studentUser

	val group = new SmallGroup
	val event = new SmallGroupEvent
	event.day = DayOfWeek.Monday
	event.startTime = LocalTime.now
	event.endTime = event.startTime.plusHours(1)
	event.group = group
	event.location  = "location"
	group.events = JArrayList(event)
	group.name = "group name"
	group.groupSet = new SmallGroupSet
	group.groupSet.format = SmallGroupFormat.Lab
	group.groupSet.module = new Module
	group.groupSet.module.code = "modcode"
	group.groupSet.name =  "groupset name"


	@Test
	def translatesFromSmallGroupEventToTimetableEvent(){
		mockSmallGroupService.findSmallGroupsByStudent(any[User]) returns (Seq(group))
	  val events = eventSource.eventsFor(student)
		events.size should be (1)
		val tte:TimetableEvent = events.head
		tte.day should be(DayOfWeek.Monday)
		tte.description should be("groupset name")
		tte.endTime should be(event.endTime)
		tte.eventType should be(TimetableEventType.Practical)
		tte.location should be(Some("location"))
		tte.moduleCode should be("modcode")
		tte.name should be("group name")
		tte.startTime should be (event.startTime)
	}

}
