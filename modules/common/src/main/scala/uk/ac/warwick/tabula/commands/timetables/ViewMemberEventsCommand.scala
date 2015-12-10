package uk.ac.warwick.tabula.commands.timetables

import org.joda.time.{DateTime, Interval, LocalDate}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentMember, Member}
import uk.ac.warwick.tabula.helpers.{Futures, Logging}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.system.permissions.{PubliclyVisiblePermissions, PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.{TimetableEvent, EventOccurrence}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object ViewMemberEventsCommand extends Logging {
	val Timeout = 15.seconds

	type TimetableCommand = Appliable[Try[Seq[EventOccurrence]]] with ViewMemberEventsRequest with SelfValidating

	def apply(member: Member, currentUser: CurrentUser) = member match {
		case student: StudentMember =>
			new ViewStudentEventsCommandInternal(student, currentUser)
				with ComposableCommand[Try[Seq[EventOccurrence]]]
				with ViewMemberEventsPermissions
				with ViewMemberEventsValidation
				with Unaudited with ReadOnly
				with AutowiringStudentTimetableEventSourceComponent
				with AutowiringScheduledMeetingEventSourceComponent
				with AutowiringTermServiceComponent
				with AutowiringTermBasedEventOccurrenceServiceComponent

		case staff: StaffMember =>
			new ViewStaffEventsCommandInternal(staff, currentUser)
				with ComposableCommand[Try[Seq[EventOccurrence]]]
				with ViewMemberEventsPermissions
				with ViewMemberEventsValidation
				with Unaudited with ReadOnly
				with AutowiringStaffTimetableEventSourceComponent
				with AutowiringScheduledMeetingEventSourceComponent
				with AutowiringTermServiceComponent
				with AutowiringTermBasedEventOccurrenceServiceComponent

		case _ =>
			logger.error(s"Don't know how to render timetables for non-student or non-staff users (${member.universityId}, ${member.userType})")
			throw new ItemNotFoundException
	}

	def public(member: Member, currentUser: CurrentUser) = member match {
		case student: StudentMember =>
			new ViewStudentEventsCommandInternal(student, currentUser)
				with ComposableCommand[Try[Seq[EventOccurrence]]]
				with PubliclyVisiblePermissions
				with ViewMemberEventsValidation
				with Unaudited with ReadOnly
				with AutowiringStudentTimetableEventSourceComponent
				with AutowiringScheduledMeetingEventSourceComponent
				with AutowiringTermServiceComponent
				with AutowiringTermBasedEventOccurrenceServiceComponent

		case staff: StaffMember =>
			new ViewStaffEventsCommandInternal(staff, currentUser)
				with ComposableCommand[Try[Seq[EventOccurrence]]]
				with PubliclyVisiblePermissions
				with ViewMemberEventsValidation
				with Unaudited with ReadOnly
				with AutowiringStaffTimetableEventSourceComponent
				with AutowiringScheduledMeetingEventSourceComponent
				with AutowiringTermServiceComponent
				with AutowiringTermBasedEventOccurrenceServiceComponent

		case _ =>
			logger.error(s"Don't know how to render timetables for non-student or non-staff users (${member.universityId}, ${member.userType})")
			throw new ItemNotFoundException
	}
}

trait MemberTimetableCommand {
	self: ViewMemberEventsRequest with TermServiceComponent with EventOccurrenceServiceComponent =>

	protected def eventsToOccurrences(events: Seq[TimetableEvent]): Seq[EventOccurrence] = {
		val dateRange = createDateRange()

		if (academicYear != null) {
			events.filter { event => event.year == academicYear }
				.flatMap(eventOccurrenceService.fromTimetableEvent(_, dateRange))
		} else {
			events
				.flatMap(eventOccurrenceService.fromTimetableEvent(_, dateRange))
		}
	}

	private def createDateRange(): Interval = {
		val startDate: DateTime =
			Option(start).map(_.toDateTimeAtStartOfDay).getOrElse(termService.getTermFromDate(academicYear.dateInTermOne).getStartDate)

		val endDate: DateTime =
			Option(end).map(_.toDateTimeAtStartOfDay).getOrElse(termService.getTermFromDate((academicYear + 1).dateInTermOne).getStartDate.minusDays(1))

		new Interval(startDate, endDate)
	}

	protected def sorted(events: Seq[EventOccurrence]) = {
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		events.sortBy(_.start)
	}

}

abstract class ViewStudentEventsCommandInternal(val member: StudentMember, currentUser: CurrentUser)
	extends CommandInternal[Try[Seq[EventOccurrence]]]
		with ViewMemberEventsRequest with MemberTimetableCommand {

	self: StudentTimetableEventSourceComponent with ScheduledMeetingEventSourceComponent with TermServiceComponent with EventOccurrenceServiceComponent =>

	def applyInternal(): Try[Seq[EventOccurrence]] = {
		val timetableOccurrences =
			studentTimetableEventSource.eventsFor(member, currentUser, TimetableEvent.Context.Student)
				.map(eventsToOccurrences)

		val meetingOccurrences = scheduledMeetingEventSource.occurrencesFor(member, currentUser, TimetableEvent.Context.Student)

		Try(Await.result(
			Futures.flatten(Seq(timetableOccurrences, meetingOccurrences)), ViewMemberEventsCommand.Timeout
		)).map(sorted)
	}

}

abstract class ViewStaffEventsCommandInternal(val member: StaffMember, currentUser: CurrentUser)
	extends CommandInternal[Try[Seq[EventOccurrence]]]
		with ViewMemberEventsRequest with MemberTimetableCommand {

	self: StaffTimetableEventSourceComponent with ScheduledMeetingEventSourceComponent with TermServiceComponent with EventOccurrenceServiceComponent =>

	def applyInternal(): Try[Seq[EventOccurrence]] = {
		val timetableOccurrences =
			staffTimetableEventSource.eventsFor(member, currentUser, TimetableEvent.Context.Staff)
				.map(eventsToOccurrences)

		val meetingOccurrences = scheduledMeetingEventSource.occurrencesFor(member, currentUser, TimetableEvent.Context.Staff)

		Try(Await.result(
			Futures.flatten(Seq(timetableOccurrences, meetingOccurrences)), ViewMemberEventsCommand.Timeout
		)).map(sorted)
	}

}

// State - unmodifiable pre-requisites
trait ViewMemberEventsState {
	val member: Member
}

// Request parameters
trait ViewMemberEventsRequest extends ViewMemberEventsState {
	var academicYear: AcademicYear = null
	var from: LocalDate = LocalDate.now.minusMonths(12)
	var to: LocalDate = from.plusMonths(13)
	def start = from
	def end = to
}

trait ViewMemberEventsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMemberEventsState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Read.Timetable, mandatory(member))
	}
}

trait ViewMemberEventsValidation extends SelfValidating {
	self: ViewMemberEventsRequest =>

	override def validate(errors: Errors) {
		// Must have either an academic year or a start and an end
		if (academicYear == null && (start == null || end == null)) {
			errors.rejectValue("academicYear", "NotEmpty")
		}
	}
}

trait ViewStaffPersonalTimetableCommandFactory {
	def apply(staffMember: StaffMember): ComposableCommand[Try[Seq[EventOccurrence]]] with ViewMemberEventsRequest
}

class ViewStaffPersonalTimetableCommandFactoryImpl(currentUser: CurrentUser)
	extends ViewStaffPersonalTimetableCommandFactory {

	def apply(staffMember: StaffMember) =
		ViewMemberEventsCommand(
			staffMember,
			currentUser
		)
}

trait ViewStudentPersonalTimetableCommandFactory {
	def apply(student: StudentMember): ComposableCommand[Try[Seq[EventOccurrence]]] with ViewMemberEventsRequest
}

class ViewStudentPersonalTimetableCommandFactoryImpl(currentUser: CurrentUser)
	extends ViewStudentPersonalTimetableCommandFactory {

	def apply(student: StudentMember) =
		ViewMemberEventsCommand(
			student,
			currentUser
		)
}
