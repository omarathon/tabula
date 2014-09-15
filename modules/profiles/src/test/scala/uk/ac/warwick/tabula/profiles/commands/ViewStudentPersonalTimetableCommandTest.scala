package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek}
import uk.ac.warwick.tabula.services.timetables.{EventOccurrenceServiceComponent, EventOccurrenceService}
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear, Mockito, TestBase}
import org.joda.time.{Interval, LocalDate, LocalDateTime, LocalTime}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEventType, TimetableEvent}

class ViewStudentPersonalTimetableCommandTest extends TestBase with Mockito{

	val testStudent = new StudentMember
	val user = mock[CurrentUser]

	val event = TimetableEvent("","","","",TimetableEventType.Induction,Nil,DayOfWeek.Monday,LocalTime.now, LocalTime.now,None,None,None,Nil,Nil, AcademicYear(2012))
	val timetableEvents = Seq(event)

	val occurrence = EventOccurrence("","", "", "", TimetableEventType.Meeting, LocalDateTime.now, LocalDateTime.now, None, None, None, Nil)
	val meetingOccurrences = Seq(occurrence)

	val earlierEvent = EventOccurrence("","","","",TimetableEventType.Induction,LocalDateTime.now.minusHours(1), LocalDateTime.now,None, None, None, Nil )
	val laterEvent = EventOccurrence("","","","",TimetableEventType.Induction,LocalDateTime.now.plusHours(1), LocalDateTime.now.plusHours(1),None, None, None, Nil )
	val eventOccurences = Seq(laterEvent,earlierEvent) // deliberately put them the wrong way round so we can check sorting

	val studentTimetableEventSource = mock[StudentTimetableEventSource]
	val scheduledMeetingEventSource = mock[ScheduledMeetingEventSource]
	val command = new ViewStudentPersonalTimetableCommandImpl(studentTimetableEventSource, scheduledMeetingEventSource, testStudent, user)  with EventOccurrenceServiceComponent{

		val eventOccurrenceService = mock[EventOccurrenceService]
	}
	command.start=  new LocalDate
	command.end = command.start.plusDays(2)
	studentTimetableEventSource.eventsFor(testStudent) returns timetableEvents
	scheduledMeetingEventSource.occurrencesFor(testStudent, user, TimetableEvent.Context.Student) returns meetingOccurrences
	command.eventOccurrenceService.fromTimetableEvent(any[TimetableEvent], any[Interval]) returns eventOccurences

	@Test
	def fetchesEventsFromEventSource(){
		command.applyInternal()
		there was one(studentTimetableEventSource).eventsFor(testStudent)
	}

	@Test
	def transformsEventsIntoOccurrences(){

		command.applyInternal()
		there was one (command.eventOccurrenceService).fromTimetableEvent(event, new Interval(command.start.toDateTimeAtStartOfDay, command.end.toDateTimeAtStartOfDay))
	}

	@Test
	def sortsOccurencesByDate(){
		val sortedEvents = command.applyInternal()
		sortedEvents should be (Seq(earlierEvent, occurrence, laterEvent))
	}

	@Test
	def requiresReadTimetablePermissions(){
		val perms = new ViewStudentTimetablePermissions with ViewStudentPersonalTimetableCommandState{
			val student = testStudent
		}

		val checking = mock[PermissionsChecking]
		perms.permissionsCheck(checking)
		there was one(checking).PermissionCheck(Permissions.Profiles.Read.Timetable, testStudent)
	}

	@Test
	def mixesCorrectPermissionsIntoCommand(){
		val composedCommand = ViewStudentPersonalTimetableCommand(studentTimetableEventSource, scheduledMeetingEventSource, testStudent, user)
		composedCommand should be(anInstanceOf[ViewStudentTimetablePermissions])
	}

}
