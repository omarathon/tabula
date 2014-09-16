package uk.ac.warwick.tabula.services.timetables

import org.joda.time.LocalTime
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent, UserLookupComponent, UserLookupService}
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

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
	event.location  = NamedLocation("location")
	group.events = JArrayList(event)
	group.name = "group name"
	group.groupSet = new SmallGroupSet
	group.groupSet.format = SmallGroupFormat.Lab
	group.groupSet.releasedToStudents = true
	group.groupSet.module = new Module
	group.groupSet.module.code = "modcode"
	group.groupSet.name =  "groupset name"


	@Test
	def translatesFromSmallGroupEventToTimetableEvent(){
		mockSmallGroupService.findSmallGroupsByStudent(any[User]) returns (Seq(group))
		mockSmallGroupService.findSmallGroupEventsByTutor(any[User]) returns (Nil)
	  val events = eventSource.eventsFor(student)
		events.size should be (1)
		val tte:TimetableEvent = events.head
		tte.day should be(DayOfWeek.Monday)
		tte.description should be("groupset name: group name")
		tte.endTime should be(event.endTime)
		tte.eventType should be(TimetableEventType.Practical)
		tte.location should be(Some("location"))
		tte.context should be(Some("MODCODE"))
		tte.name should be("groupset name")
		tte.startTime should be (event.startTime)
	}

}
