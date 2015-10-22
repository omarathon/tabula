package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.timetables.TimetableEvent

import scala.util.{Try, Success}

trait StudentTimetableEventSource extends TimetableEventSource[StudentMember] {
	override def eventsFor(student: StudentMember, currentUser: CurrentUser, context: TimetableEvent.Context): Try[Seq[TimetableEvent]]
}

trait StudentTimetableEventSourceComponent {
	def studentTimetableEventSource: StudentTimetableEventSource
}

trait CombinedStudentTimetableEventSourceComponent extends StudentTimetableEventSourceComponent {
	self: StaffAndStudentTimetableFetchingServiceComponent with SmallGroupEventTimetableEventSourceComponent =>

	def studentTimetableEventSource: StudentTimetableEventSource = new CombinedStudentTimetableEventSource

	class CombinedStudentTimetableEventSource() extends StudentTimetableEventSource {
		def eventsFor(student: StudentMember, currentUser: CurrentUser, context: TimetableEvent.Context): Try[Seq[TimetableEvent]] = {
			val timetableEvents: Try[Seq[TimetableEvent]] = timetableFetchingService.getTimetableForStudent(student.universityId)
			val smallGroupEvents: Try[Seq[TimetableEvent]] = studentGroupEventSource.eventsFor(student, currentUser, context)

			val staffEvents: Try[Seq[TimetableEvent]] =
				if (student.isPGR) { timetableFetchingService.getTimetableForStaff(student.universityId) }
				else Success(Nil)

			Try(Seq(timetableEvents, smallGroupEvents, staffEvents).flatMap(_.get))
		}
	}

}

trait AutowiringStudentTimetableEventSourceComponent extends StudentTimetableEventSourceComponent {
	val studentTimetableEventSource = (new CombinedStudentTimetableEventSourceComponent
		with SmallGroupEventTimetableEventSourceComponentImpl
		with CombinedHttpTimetableFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringScientiaConfigurationComponent
		with AutowiringCelcatConfigurationComponent
		with AutowiringSecurityServiceComponent
		with SystemClockComponent
	).studentTimetableEventSource
}
