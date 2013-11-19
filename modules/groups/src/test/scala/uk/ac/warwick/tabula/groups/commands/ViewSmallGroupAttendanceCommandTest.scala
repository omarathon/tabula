package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.services.TermServiceComponent
import uk.ac.warwick.tabula.services.SmallGroupServiceComponent
import uk.ac.warwick.tabula.services.TermService
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import org.joda.time.LocalTime
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import SmallGroupAttendanceState._

class ViewSmallGroupAttendanceCommandTest extends TestBase with Mockito {
	
	trait CommandTestSupport extends SmallGroupServiceComponent with TermServiceComponent {
		val smallGroupService = mock[SmallGroupService]
		val termService = mock[TermService]
	}
	
	trait Fixture {
		val userLookup = new MockUserLookup
		
		val set = new SmallGroupSet
		set.academicYear = AcademicYear.guessByDate(DateTime.now)
		
		val group = new SmallGroup(set)
		group._studentsGroup.userLookup = userLookup
		
		// The group has two events. Event 1 runs at Monday 11am on week 2, 3 and 4; Event 2 runs at Monday 3pm on weeks 1, 3 and 7
		val event1 = new SmallGroupEvent(group)
		event1.day = DayOfWeek.Monday
		event1.startTime = new LocalTime(11, 0)
		event1.weekRanges = Seq(WeekRange(2, 4))
		
		val event2 = new SmallGroupEvent(group)
		event2.day = DayOfWeek.Monday
		event2.startTime = new LocalTime(15, 0)
		event2.weekRanges = Seq(WeekRange(1), WeekRange(3), WeekRange(7))
		
		group.events.add(event1)
		group.events.add(event2)
	}
	
	@Test
	def commandApplyNoData() { new Fixture() {
		val command = new ViewSmallGroupAttendanceCommand(group) with CommandTestSupport
		
		command.smallGroupService.findAttendanceByGroup(group) returns (Seq())
		
		val info = command.applyInternal()
		
		info.instances should be (Seq(
			(event2, 1),
			(event1, 2),
			(event1, 3),
			(event2, 3),
			(event1, 4),
			(event2, 7)
		))
		info.attendance should be ('empty)
	}}
	
	@Test
	def commandApply() { new Fixture() {
		userLookup.registerUsers("user1", "user2", "user3", "user4", "user5")
		
		val user1 = userLookup.getUserByUserId("user1")
		val user2 = userLookup.getUserByUserId("user2")
		val user3 = userLookup.getUserByUserId("user3")
		val user4 = userLookup.getUserByUserId("user4")
		val user5 = userLookup.getUserByUserId("user5")
		
		group._studentsGroup.add(user1)
		group._studentsGroup.add(user2)
		group._studentsGroup.add(user3)
		group._studentsGroup.add(user4)
		
		// user5 turned up to the first occurrence and then left
		
		// Recorded attendance for week 1 and both in 3 - rest haven't happened yet, 2 is missing
		
		// Everyone turned up for week 1
		val occurrence1 = new SmallGroupEventOccurrence
		occurrence1.attendees.userLookup = userLookup
		occurrence1.event = event2
		occurrence1.week = 1
		occurrence1.attendees.add(user1)
		occurrence1.attendees.add(user2)
		occurrence1.attendees.add(user3)
		occurrence1.attendees.add(user4)
		occurrence1.attendees.add(user5)
		
		// User3 missed the first seminar in week 3, user4 missed the second
		val occurrence2 = new SmallGroupEventOccurrence
		occurrence2.attendees.userLookup = userLookup
		occurrence2.event = event1
		occurrence2.week = 3
		occurrence2.attendees.add(user1)
		occurrence2.attendees.add(user2)
		occurrence2.attendees.add(user4)
		
		val occurrence3 = new SmallGroupEventOccurrence
		occurrence3.attendees.userLookup = userLookup
		occurrence3.event = event2
		occurrence3.week = 3
		occurrence3.attendees.add(user1)
		occurrence3.attendees.add(user2)
		occurrence3.attendees.add(user3)
		
		val command = new ViewSmallGroupAttendanceCommand(group) with CommandTestSupport
		command.smallGroupService.findAttendanceByGroup(group) returns (Seq(occurrence1, occurrence2, occurrence3))
		command.termService.getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear]) returns (4)
		
		val info = command.applyInternal()
		
		info.instances should be (Seq(
			(event2, 1),
			(event1, 2),
			(event1, 3),
			(event2, 3),
			(event1, 4),
			(event2, 7)
		))
		
		// Map all the SortedMaps to Seqs to preserve the order they've been set as
		val userAttendanceSeqs = info.attendance.toSeq.map { case (user, attendance) =>
			(user -> attendance.toSeq)
		}
		
		userAttendanceSeqs should be (Seq(
			(user1, Seq(
				((event2, 1), Attended),
				((event1, 2), Late),
				((event1, 3), Attended),
				((event2, 3), Attended),
				((event1, 4), NotRecorded),
				((event2, 7), NotRecorded)
			)),
			
			(user2, Seq(
				((event2, 1), Attended),
				((event1, 2), Late),
				((event1, 3), Attended),
				((event2, 3), Attended),
				((event1, 4), NotRecorded),
				((event2, 7), NotRecorded)
			)),
			
			(user3, Seq(
				((event2, 1), Attended),
				((event1, 2), Late),
				((event1, 3), Missed),
				((event2, 3), Attended),
				((event1, 4), NotRecorded),
				((event2, 7), NotRecorded)
			)),
			
			(user4, Seq(
				((event2, 1), Attended),
				((event1, 2), Late),
				((event1, 3), Attended),
				((event2, 3), Missed),
				((event1, 4), NotRecorded),
				((event2, 7), NotRecorded)
			)),
			
			(user5, Seq(
				((event2, 1), Attended),
				((event1, 2), Late),
				((event1, 3), Missed), // TODO because the user isn't in the group, is this really missed? Hard to tell.
				((event2, 3), Missed), // TODO because the user isn't in the group, is this really missed? Hard to tell.
				((event1, 4), NotRecorded),
				((event2, 7), NotRecorded)
			))
		))
	}}
	
	@Test
	def tab1534() {
		// An intricacy of the way that SortedMap works means that if you have two 
		// keys with an identical sort order, they'll get merged into a single key.
		// This means that we risk students with the same name getting merged into one.
		// Bad!
		val userLookup = new MockUserLookup
		
		val set = new SmallGroupSet
		set.academicYear = AcademicYear.guessByDate(DateTime.now)
		
		val group = new SmallGroup(set)
		group._studentsGroup.userLookup = userLookup
		
		val event = new SmallGroupEvent(group)
		event.day = DayOfWeek.Monday
		event.startTime = new LocalTime(11, 0)
		event.weekRanges = Seq(WeekRange(2, 4))
		
		group.events.add(event)
		
		userLookup.registerUsers("user1", "user2", "user3")
		
		val user1 = userLookup.getUserByUserId("user1")
		val user2 = userLookup.getUserByUserId("user2")
		val user3 = userLookup.getUserByUserId("user3")
		
		// Give user2 and user3 the same name
		user2.setFirstName("Billy")
		user2.setLastName("Sameson")
		user2.setFullName("Billy Sameson")
		
		user3.setFirstName("Billy")
		user3.setLastName("Sameson")
		user3.setFullName("Billy Sameson")
		
		val occurrence = new SmallGroupEventOccurrence
		occurrence.attendees.userLookup = userLookup
		occurrence.event = event
		occurrence.week = 1
		occurrence.attendees.add(user1)
		occurrence.attendees.add(user2)
		occurrence.attendees.add(user3)
		
		val command = new ViewSmallGroupAttendanceCommand(group) with CommandTestSupport
		command.smallGroupService.findAttendanceByGroup(group) returns (Seq(occurrence))
		command.termService.getAcademicWeekForAcademicYear(any[DateTime], any[AcademicYear]) returns (4)
		
		val info = command.applyInternal()
		info.attendance.keySet.size should be (3) // If it's 2, we're bad
	}

}