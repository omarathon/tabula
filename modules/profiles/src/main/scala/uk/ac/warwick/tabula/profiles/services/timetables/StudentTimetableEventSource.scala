package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.timetables.TimetableEvent

trait StudentTimetableEventSource {
	def eventsFor(student: StudentMember):Seq[TimetableEvent]
}
trait StudentTimetableEventSourceComponent {
	def studentTimetableEventSource: StudentTimetableEventSource
}

trait CombinedStudentTimetableEventSourceComponent extends StudentTimetableEventSourceComponent {
	self: StaffAndStudentTimetableFetchingServiceComponent with SmallGroupEventTimetableEventSourceComponent =>

	def studentTimetableEventSource: StudentTimetableEventSource = new CombinedStudentTimetableEventSource

	class CombinedStudentTimetableEventSource() extends StudentTimetableEventSource {
		def eventsFor(student: StudentMember): Seq[TimetableEvent] = {
			val events = timetableFetchingService.getTimetableForStudent(student.universityId) ++
				studentGroupEventSource.eventsFor(student)
			if (student.isPGR) { events ++ timetableFetchingService.getTimetableForStaff(student.universityId) }
			events
		}
	}

}

