package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.timetables.TimetableEvent

trait StaffTimetableEventSource {
	def eventsFor(staff: StaffMember, currentUser: CurrentUser, context: TimetableEvent.Context): Seq[TimetableEvent]
}
trait StaffTimetableEventSourceComponent {
	def staffTimetableEventSource: StaffTimetableEventSource
}

trait CombinedStaffTimetableEventSourceComponent extends StaffTimetableEventSourceComponent {
	this: StaffTimetableFetchingServiceComponent with SmallGroupEventTimetableEventSourceComponent =>

	def staffTimetableEventSource: StaffTimetableEventSource = new CombinedStaffTimetableEventSource

	class CombinedStaffTimetableEventSource() extends StaffTimetableEventSource {

		def eventsFor(staff: StaffMember, currentUser: CurrentUser, context: TimetableEvent.Context): Seq[TimetableEvent] = {
			val timetableEvents: Seq[TimetableEvent] = timetableFetchingService.getTimetableForStaff(staff.universityId).getOrElse(Nil)
			val smallGroupEvents: Seq[TimetableEvent] = staffGroupEventSource.eventsFor(staff, currentUser, context)

			timetableEvents ++ smallGroupEvents
		}

	}
}
