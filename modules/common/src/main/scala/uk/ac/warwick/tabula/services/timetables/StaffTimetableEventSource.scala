package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.timetables.TimetableEvent

import scala.util.Try

trait StaffTimetableEventSource extends TimetableEventSource[StaffMember] {
	override def eventsFor(staff: StaffMember, currentUser: CurrentUser, context: TimetableEvent.Context): Try[Seq[TimetableEvent]]
}

trait StaffTimetableEventSourceComponent {
	def staffTimetableEventSource: StaffTimetableEventSource
}

trait CombinedStaffTimetableEventSourceComponent extends StaffTimetableEventSourceComponent {
	this: StaffTimetableFetchingServiceComponent with SmallGroupEventTimetableEventSourceComponent =>

	def staffTimetableEventSource: StaffTimetableEventSource = new CombinedStaffTimetableEventSource

	class CombinedStaffTimetableEventSource() extends StaffTimetableEventSource {

		def eventsFor(staff: StaffMember, currentUser: CurrentUser, context: TimetableEvent.Context): Try[Seq[TimetableEvent]] = {
			val timetableEvents: Try[Seq[TimetableEvent]] = timetableFetchingService.getTimetableForStaff(staff.universityId)
			val smallGroupEvents: Try[Seq[TimetableEvent]] = staffGroupEventSource.eventsFor(staff, currentUser, context)

			Try(Seq(timetableEvents, smallGroupEvents).flatMap(_.get))
		}

	}
}

trait AutowiringStaffTimetableEventSourceComponent extends StaffTimetableEventSourceComponent {
	val staffTimetableEventSource = (new CombinedStaffTimetableEventSourceComponent
		with SmallGroupEventTimetableEventSourceComponentImpl
		with CombinedHttpTimetableFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringScientiaConfigurationComponent
		with AutowiringCelcatConfigurationComponent
		with AutowiringSecurityServiceComponent
		with SystemClockComponent
	).staffTimetableEventSource
}