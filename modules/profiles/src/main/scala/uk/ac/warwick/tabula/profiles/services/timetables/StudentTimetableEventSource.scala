package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.data.model.StudentMember

trait StudentTimetableEventSource {
	def eventsFor(student: StudentMember):Seq[TimetableEvent]
}
trait StudentTimetableEventSourceComponent {
	def studentTimetableEventSource: StudentTimetableEventSource
}

trait CombinedStudentTimetableEventSourceComponent extends StudentTimetableEventSourceComponent {
	this: TimetableFetchingServiceComponent with SmallGroupEventTimetableEventSourceComponent =>

	def studentTimetableEventSource: StudentTimetableEventSource = new CombinedStudentTimetableEventSource

	class CombinedStudentTimetableEventSource() extends StudentTimetableEventSource{

		def eventsFor(student: StudentMember): Seq[TimetableEvent] = {
			timetableFetchingService.getTimetableForStudent(student.universityId) ++
				studentGroupEventSource.eventsFor(student)
		}
	}

}


