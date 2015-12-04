package uk.ac.warwick.tabula.api.web.controllers.timetables

import javax.validation.Valid

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property._
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.services.timetables.{AutowiringTermBasedEventOccurrenceServiceComponent, EventOccurrenceServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.{ItemNotFoundException, CurrentUser, AcademicYear, RequestFailedException}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.commands.timetables.{ViewMemberEventsCommand, ViewMemberEventsRequest}
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentMember, Member}
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.timetables.EventOccurrence
import uk.ac.warwick.tabula.web.views.{IcalView, FullCalendarEvent, JSONView, JSONErrorView}

import scala.util.{Failure, Success, Try}

import MemberCalendarController._

object MemberCalendarController {
	type TimetableCommand = Appliable[Try[Seq[EventOccurrence]]] with ViewMemberEventsRequest
}

@Controller
@RequestMapping(Array("/v1/member/{member}/timetable/calendar", "/v1/member/{member}/timetable/calendar.*"))
class MemberCalendarController extends ApiController
	with GetMemberCalendarJsonApi
	with GetMemberCalendarIcalApi
	with PathVariableMemberCalendarApi
	with GeneratesTimetableIcalFeed
	with AutowiringUserLookupComponent
	with AutowiringTermBasedEventOccurrenceServiceComponent
	with AutowiringTermServiceComponent {
	validatesSelf[SelfValidating]
}

@Controller
@RequestMapping(Array("/v1/timetable/calendar/{timetableHash}.ics"))
class MemberTimetableHashCalendarController extends ApiController
	with GetMemberCalendarIcalApi
	with PathVariableTimetableHashMemberCalendarApi
	with GeneratesTimetableIcalFeed
	with AutowiringUserLookupComponent
	with AutowiringTermBasedEventOccurrenceServiceComponent
	with AutowiringTermServiceComponent
	with AutowiringProfileServiceComponent {
	validatesSelf[SelfValidating]
}

sealed trait MemberCalendarApi

trait PathVariableMemberCalendarApi extends MemberCalendarApi {
	self: PermissionsCheckingMethods =>

	@ModelAttribute("getTimetableCommand")
	def getTimetableCommand(@PathVariable member: Member, user: CurrentUser): TimetableCommand =
		ViewMemberEventsCommand(mandatory(member), user)

}

trait PathVariableTimetableHashMemberCalendarApi extends MemberCalendarApi {
	self: ProfileServiceComponent =>

	@ModelAttribute("getTimetableCommand")
	def getTimetableCommand(@PathVariable timetableHash: String): TimetableCommand = {
		// Use a mocked up CurrentUser, as the actual current user is probably not logged in
		def currentUser(m: Member) =
			new CurrentUser(
				realUser = m.asSsoUser.tap { _.setIsLoggedIn(true) },
				apparentUser = m.asSsoUser.tap { _.setIsLoggedIn(true) },
				profile = Some(m),
				sysadmin = false,
				masquerader = false,
				god = false
			)

		profileService.getMemberByTimetableHash(timetableHash).map {
			case student: StudentMember => ViewMemberEventsCommand.public(student, currentUser(student))
			case staff: StaffMember => ViewMemberEventsCommand.public(staff, currentUser(staff))
		}.getOrElse(throw new ItemNotFoundException)
	}

}

trait GetMemberCalendarJsonApi {
	self: ApiController
		with MemberCalendarApi
		with UserLookupComponent =>

	def colourEvents(uncoloured: Seq[FullCalendarEvent]): Seq[FullCalendarEvent] = {
		val colours = Seq("#239b92", "#a3b139", "#ec8d22", "#ef3e36", "#df4094", "#4daacc", "#167ec2", "#f1592a", "#818285")
		// an infinitely repeating stream of colours
		val colourStream = Stream.continually(colours.toStream).flatten
		val contexts = uncoloured.map(_.parentShortName).distinct
		val contextsWithColours = contexts.zip(colourStream)
		uncoloured.map { event =>
			if (event.title == "Busy") {
				// FIXME hack
				event.copy(backgroundColor = "#bbb", borderColor = "#bbb")
			} else {
				val colour = contextsWithColours.find(_._1 == event.parentShortName).get._2
				event.copy(backgroundColor = colour, borderColor = colour)
			}
		}
	}

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def showMemberTimetable(@Valid @ModelAttribute("getTimetableCommand") command: TimetableCommand, errors: Errors) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else command.apply() match {
			case Success(events) => Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"events" -> colourEvents(events.map(FullCalendarEvent(_, userLookup)))
			)))
			case Failure(t) => throw new RequestFailedException("The timetabling service could not be reached", t)
		}
	}
}

trait GetMemberCalendarIcalApi {
	self: ApiController
		with MemberCalendarApi
		with UserLookupComponent
		with GeneratesTimetableIcalFeed =>

	@RequestMapping(method = Array(GET), produces = Array("text/calendar"))
	def icalMemberTimetable(@Valid @ModelAttribute("getTimetableCommand") command: TimetableCommand) = {
		val cal = getIcalFeed(command)
		Mav(new IcalView(cal), "filename" -> s"${command.member.universityId}.ics")
	}

}

trait GeneratesTimetableIcalFeed {
	self: TermServiceComponent with EventOccurrenceServiceComponent =>

	def getIcalFeed(command: TimetableCommand): Calendar = {
		val year = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		// Start from either 1 week ago, or the start of the current academic year, whichever is earlier
		val start = {
			val startOfYear = termService.getTermFromAcademicWeek(1, year).getStartDate.toLocalDate
			val oneWeekAgo = DateTime.now.minusWeeks(1).toLocalDate

			if (startOfYear.isBefore(oneWeekAgo)) startOfYear else oneWeekAgo
		}

		// End either at the end of the current academic year, or in 15 weeks time, whichever is later
		val end = {
			val endOfYear = termService.getTermFromAcademicWeek(1, year + 1).getStartDate.toLocalDate
			val fifteenWeeksTime = DateTime.now.plusWeeks(15).toLocalDate

			if (endOfYear.isAfter(fifteenWeeksTime)) endOfYear else fifteenWeeksTime
		}

		command.from = start
		command.to = end

		val timetableEvents = command.apply().get

		val cal: Calendar = new Calendar
		cal.getProperties.add(Version.VERSION_2_0)
		cal.getProperties.add(new ProdId("-//Tabula//University of Warwick IT Services//EN"))
		cal.getProperties.add(CalScale.GREGORIAN)
		cal.getProperties.add(Method.PUBLISH)
		cal.getProperties.add(new XProperty("X-PUBLISHED-TTL", "PT12H"))
		cal.getProperties.add(new XProperty("X-WR-CALNAME", s"Tabula timetable - ${command.member.universityId}"))

		for (event <- timetableEvents) {
			val vEvent: VEvent = eventOccurrenceService.toVEvent(event)
			cal.getComponents.add(vEvent)
		}

		// TAB-2722 Empty calendars throw a validation exception
		// Add Xmas day to get around this
		if (timetableEvents.isEmpty) {
			val xmasVEvent = new VEvent(
				new net.fortuna.ical4j.model.DateTime(new DateTime(DateTime.now.getYear, 12, 25, 0, 0).getMillis),
				new net.fortuna.ical4j.model.DateTime(new DateTime(DateTime.now.getYear, 12, 25, 0, 0).getMillis),
				"Christmas day"
			)
			xmasVEvent.getProperties.add(new Organizer("MAILTO:no-reply@tabula.warwick.ac.uk"))
			xmasVEvent.getProperties.add(new Uid("Tabula-Stub-Xmas"))
			cal.getComponents.add(xmasVEvent)
		}

		cal
	}

}