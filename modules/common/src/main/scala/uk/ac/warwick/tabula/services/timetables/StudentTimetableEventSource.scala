package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.helpers.{Futures, SystemClockComponent}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.timetables.TimetableEvent

import scala.concurrent.Future

trait StudentTimetableEventSource extends TimetableEventSource[StudentMember] {
	override def eventsFor(student: StudentMember, currentUser: CurrentUser, context: TimetableEvent.Context): Future[Seq[TimetableEvent]]
}

trait StudentTimetableEventSourceComponent {
	def studentTimetableEventSource: StudentTimetableEventSource
}

trait CombinedStudentTimetableEventSourceComponent extends StudentTimetableEventSourceComponent {
	self: StaffAndStudentTimetableFetchingServiceComponent with SmallGroupEventTimetableEventSourceComponent =>

	def studentTimetableEventSource: StudentTimetableEventSource = new CombinedStudentTimetableEventSource

	class CombinedStudentTimetableEventSource() extends StudentTimetableEventSource {
		def eventsFor(student: StudentMember, currentUser: CurrentUser, context: TimetableEvent.Context): Future[Seq[TimetableEvent]] = {
			val timetableEvents: Future[Seq[TimetableEvent]] = timetableFetchingService.getTimetableForStudent(student.universityId)
			val smallGroupEvents: Future[Seq[TimetableEvent]] = studentGroupEventSource.eventsFor(student, currentUser, context)

			val staffEvents: Future[Seq[TimetableEvent]] =
				if (student.isPGR) { timetableFetchingService.getTimetableForStaff(student.universityId) }
				else Future.successful(Nil)

			Futures.flatten(Seq(timetableEvents, smallGroupEvents, staffEvents))
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
