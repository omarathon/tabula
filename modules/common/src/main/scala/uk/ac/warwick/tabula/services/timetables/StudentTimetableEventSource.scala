package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.timetables.TimetableEvent

trait StudentTimetableEventSource {
	def eventsFor(student: StudentMember, currentUser: CurrentUser, context: TimetableEvent.Context): Seq[TimetableEvent]
}
trait StudentTimetableEventSourceComponent {
	def studentTimetableEventSource: StudentTimetableEventSource
}

trait CombinedStudentTimetableEventSourceComponent extends StudentTimetableEventSourceComponent {
	self: StaffAndStudentTimetableFetchingServiceComponent with SmallGroupEventTimetableEventSourceComponent =>

	def studentTimetableEventSource: StudentTimetableEventSource = new CombinedStudentTimetableEventSource

	class CombinedStudentTimetableEventSource() extends StudentTimetableEventSource {
		def eventsFor(student: StudentMember, currentUser: CurrentUser, context: TimetableEvent.Context): Seq[TimetableEvent] = {
			val events = timetableFetchingService.getTimetableForStudent(student.universityId) ++
				studentGroupEventSource.eventsFor(student, currentUser, context)
			if (student.isPGR) { events ++ timetableFetchingService.getTimetableForStaff(student.universityId) }
			events
		}
	}

}

