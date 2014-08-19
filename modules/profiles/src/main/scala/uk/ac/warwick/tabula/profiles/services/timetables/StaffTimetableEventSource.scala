package uk.ac.warwick.tabula.profiles.services.timetables

import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.timetables.TimetableEvent

trait StaffTimetableEventSource {
	def eventsFor(staff: StaffMember):Seq[TimetableEvent]
}
trait StaffTimetableEventSourceComponent {
	def staffTimetableEventSource: StaffTimetableEventSource
}

trait CombinedStaffTimetableEventSourceComponent extends StaffTimetableEventSourceComponent {
	this: StaffTimetableFetchingServiceComponent with SmallGroupEventTimetableEventSourceComponent =>

	def staffTimetableEventSource: StaffTimetableEventSource = new CombinedStaffTimetableEventSource

	class CombinedStaffTimetableEventSource() extends StaffTimetableEventSource {

		def eventsFor(staff: StaffMember): Seq[TimetableEvent] = {
			timetableFetchingService.getTimetableForStaff(staff.universityId) ++
				staffGroupEventSource.eventsFor(staff)
		}

	}
}
