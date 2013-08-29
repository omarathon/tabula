package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.profiles.services.timetables._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import org.joda.time.{Interval, LocalDate, LocalDateTime, LocalTime}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions

class ViewStudentPersonalTimetableCommandTest extends TestBase with Mockito{

	val student = new StudentMember
	val event = TimetableEvent("","",TimetableEventType.Induction,Nil,DayOfWeek.Monday,LocalTime.now, LocalTime.now,None,"",Nil)
	val timetableEvents = Seq(event)
	val earlierEvent = EventOccurrence("","",TimetableEventType.Induction,LocalDateTime.now, LocalDateTime.now,None, "", Nil )
	val laterEvent = EventOccurrence("","",TimetableEventType.Induction,LocalDateTime.now.plusHours(1), LocalDateTime.now.plusHours(1),None, "", Nil )
	val eventOccurences = Seq(laterEvent,earlierEvent) // deliberately put them the wrong way round so we can check sorting

	val command = new ViewStudentPersonalTimetableCommandImpl with StudentTimetableEventSourceComponent with EventOccurrenceServiceComponent{
		val studentTimetableEventSource = mock[StudentTimetableEventSource]
		val eventOccurrenceService = mock[EventOccurrenceService]
	}
	command.student = student
	command.start=  new LocalDate
	command.end = command.start.plusDays(2)
	command.studentTimetableEventSource.eventsFor(student) returns timetableEvents
	command.eventOccurrenceService.fromTimetableEvent(any[TimetableEvent], any[Interval]) returns eventOccurences

	@Test
	def fetchesEventsFromEventSource(){
		command.applyInternal()
		there was one(command.studentTimetableEventSource).eventsFor(student)
	}

	@Test
	def transformsEventsIntoOccurrences(){

		command.applyInternal()
		there was one (command.eventOccurrenceService).fromTimetableEvent(event, new Interval(command.start.toDateTimeAtStartOfDay, command.end.toDateTimeAtStartOfDay))
	}

	@Test
	def sortsOccurencesByDate(){
		val sortedEvents = command.applyInternal()
		sortedEvents.size should be(2)
		sortedEvents.head should be (earlierEvent)
		sortedEvents.last should be (laterEvent)
	}

	@Test
	def requiresReadTimetablePermissions(){
		val perms = new ViewStudentTimetablePermissions with ViewStudentPersonalTimetableCommandState
		perms.student = student
		val checking = mock[PermissionsChecking]
		perms.permissionsCheck(checking)
		there was one(checking).PermissionCheck(Permissions.Profiles.Read.Timetable, student)
	}

	@Test
	def mixesCorrectPermissionsIntoCommand(){
		val composedCommand = ViewStudentPersonalTimetableCommand()
		composedCommand should be(anInstanceOf[ViewStudentTimetablePermissions])
	}
}
