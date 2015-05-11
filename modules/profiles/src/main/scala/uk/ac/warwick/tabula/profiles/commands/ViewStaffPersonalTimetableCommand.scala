package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.Interval
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, Command, CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.{AutowiringTermBasedEventOccurrenceServiceComponent, EventOccurrenceServiceComponent, ScheduledMeetingEventSource, StaffTimetableEventSource}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, Public, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}

// Do not remove
// Should be import uk.ac.warwick.tabula.helpers.DateTimeOrdering

trait ViewStaffPersonalTimetableCommandState extends PersonalTimetableCommandState {
	val staff: StaffMember
	lazy val member = staff
}

/*
 * If you want to add new sources of events to the calendar, here's where to do it:
 *
 *  - if your events recur throughout the academic year, and can be described in terms of "on this day, at this time,
 *  in these academic weeks", then implement TimetableEventSource, and register your source with
 *  CombinedStudentTimetableEventSource.
 *
 *  - if your events are one-off, but still described in terms of day, time, and academic week, implement a
 *  TimetableEventSource as above (producing a Seq of size 1) and plumb it in as above; it will save you having to
 *  write code to infer a proper calendar date.
 *
 *  - If your events already have a calendar date associated with them, then you should implement a method which
 *  produces a Seq[EventOccurrence]. Invoke that within this class's applyInternal, and add the result to the
 *  "occurrences" list, before the list is sorted.
 *
 *  - If there are several sources that fit the last category, then it would make sense to wrap them all into a
 *  per-student "NonRecurringEventSource", add a cache, and pass that into this class's constructor alongside
 *  the StudentTimetableEventSource
 *
 */
class ViewStaffPersonalTimetableCommandImpl (
	staffTimetableEventSource: StaffTimetableEventSource,
	scheduledMeetingEventSource: ScheduledMeetingEventSource,
	val staff: StaffMember,
	val currentUser: CurrentUser
) extends CommandInternal[Seq[EventOccurrence]] with ViewStaffPersonalTimetableCommandState {
	this: EventOccurrenceServiceComponent =>

	def eventsToOccurrences: TimetableEvent => Seq[EventOccurrence] =
		eventOccurrenceService.fromTimetableEvent(_, new Interval(start.toDateTimeAtStartOfDay, end.toDateTimeAtStartOfDay))

	def applyInternal(): Seq[EventOccurrence] = {
		val timetableEvents = staffTimetableEventSource.eventsFor(staff, currentUser, TimetableEvent.Context.Staff)
		val occurrences =
			timetableEvents.flatMap(eventsToOccurrences) ++
				scheduledMeetingEventSource.occurrencesFor(staff, currentUser, TimetableEvent.Context.Staff)

		// Converter to make localDates sortable
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		occurrences.sortBy(_.start)
	}


}


trait ViewStaffTimetablePermissions extends RequiresPermissionsChecking{
	this:ViewStaffPersonalTimetableCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Read.Timetable, staff)
	}
}


object ViewStaffPersonalTimetableCommand {

	def apply(
		staffTimetableEventSource:StaffTimetableEventSource,
		scheduledMeetingEventSource: ScheduledMeetingEventSource,
		staff: StaffMember,
		currentUser: CurrentUser
	) = new ViewStaffPersonalTimetableCommandImpl(staffTimetableEventSource, scheduledMeetingEventSource, staff, currentUser)
			with ComposableCommand[Seq[EventOccurrence]]
			with ViewStaffTimetablePermissions
			with ReadOnly with Unaudited
			with AutowiringTermBasedEventOccurrenceServiceComponent
			with TermAwareWeekToDateConverterComponent
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
			with ViewStaffPersonalTimetableCommandState
}

object PublicStaffPersonalTimetableCommand {

	def apply(
		staffTimetableEventSource: StaffTimetableEventSource,
		scheduledMeetingEventSource: ScheduledMeetingEventSource,
		staff: StaffMember,
		currentUser: CurrentUser
	): Appliable[Seq[EventOccurrence]] with PersonalTimetableCommandState =
		new ViewStaffPersonalTimetableCommandImpl(staffTimetableEventSource, scheduledMeetingEventSource, staff, currentUser)
			with Command[Seq[EventOccurrence]]
			with Public
			with ReadOnly with Unaudited
			with AutowiringTermBasedEventOccurrenceServiceComponent
			with TermAwareWeekToDateConverterComponent
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
			with ViewStaffPersonalTimetableCommandState
}

trait ViewStaffPersonalTimetableCommandFactory {
	def apply(staffMember: StaffMember): ComposableCommand[Seq[EventOccurrence]]
}
class ViewStaffPersonalTimetableCommandFactoryImpl(
	staffTimetableEventSource: StaffTimetableEventSource,
	scheduledMeetingEventSource: ScheduledMeetingEventSource,
	currentUser: CurrentUser
) extends ViewStaffPersonalTimetableCommandFactory {
	def apply(staffMember: StaffMember) =
		ViewStaffPersonalTimetableCommand(
			staffTimetableEventSource,
			scheduledMeetingEventSource,
			staffMember,
			currentUser
		)
}
