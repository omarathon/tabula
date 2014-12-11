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
			val timetableEvents: Seq[TimetableEvent] = timetableFetchingService.getTimetableForStudent(student.universityId).getOrElse(Nil)
			val smallGroupEvents: Seq[TimetableEvent] = studentGroupEventSource.eventsFor(student, currentUser, context)

			val staffEvents: Seq[TimetableEvent] =
				if (student.isPGR) { timetableFetchingService.getTimetableForStaff(student.universityId).getOrElse(Nil) }
				else Nil

			timetableEvents ++ smallGroupEvents ++ staffEvents
		}
	}

}

