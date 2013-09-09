package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.{AcademicYear, TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import org.joda.time.LocalTime

class CombinedStudentTimetableEventSourceTest extends TestBase with Mockito{

	val student = new StudentMember
	student.universityId = "university ID"

	val ttEvent= TimetableEvent("From Timetable","",TimetableEventType.Induction,Nil,DayOfWeek.Monday,LocalTime.now, LocalTime.now,None,"",Nil,AcademicYear(2013))
	val timetableEvents = Seq(ttEvent)

	val sgEvent= TimetableEvent("From Group","",TimetableEventType.Induction,Nil,DayOfWeek.Monday,LocalTime.now, LocalTime.now,None,"",Nil,AcademicYear(2013))
	val groupEvents = Seq(sgEvent)

	val source = new CombinedStudentTimetableEventSourceComponent
		with TimetableFetchingServiceComponent
		with SmallGroupEventTimetableEventSourceComponent{

		val studentGroupEventSource = mock[StudentTimetableEventSource]
		val timetableFetchingService = mock[TimetableFetchingService]
	}

	source.timetableFetchingService.getTimetableForStudent(student.universityId)  returns timetableEvents
	source.studentGroupEventSource.eventsFor(student) returns groupEvents

	@Test
	def callsBothServicesAndAggregatesTheResult(){
		source.studentTimetableEventSource.eventsFor(student) should be (timetableEvents ++ groupEvents)
	}


}
