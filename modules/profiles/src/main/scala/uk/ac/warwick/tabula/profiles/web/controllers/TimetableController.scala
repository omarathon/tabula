package uk.ac.warwick.tabula.profiles.web.controllers

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property._
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.timetables.{ViewMemberEventsCommand, ViewMemberEventsRequest}
import uk.ac.warwick.tabula.data.model.{Member, StaffMember, StudentMember}
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.profiles.web.views.FullCalendarEvent
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.timetables.EventOccurrence
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{IcalView, JSONView}
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.JavaImports._

import scala.util.Try

abstract class AbstractTimetableController extends ProfilesController {
	self: ProfileServiceComponent =>

	type TimetableCommand = Appliable[Try[Seq[EventOccurrence]]] with ViewMemberEventsRequest

	protected def commandForMember(whoFor: Member): TimetableCommand =
		ViewMemberEventsCommand(whoFor, user)

	protected def commandForTimetableHash(timetableHash: String): TimetableCommand = {
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

@Controller
@RequestMapping(value = Array("/timetable/api"))
class TimetableController extends AbstractTimetableController
	with AutowiringUserLookupComponent
	with AutowiringProfileServiceComponent {

	@ModelAttribute("command")
	def command(@RequestParam(value="whoFor") whoFor: Member) = commandForMember(whoFor)

	@RequestMapping
	def getEvents(
		@ModelAttribute("command") command: TimetableCommand
	): Mav = {
		val timetableEvents = command.apply().get
		val calendarEvents = timetableEvents.map (FullCalendarEvent(_, userLookup))
		Mav(new JSONView(colourEvents(calendarEvents)))
	}

	def colourEvents(uncoloured: Seq[FullCalendarEvent]):Seq[FullCalendarEvent] = {
		val colours = Seq("#239b92","#a3b139","#ec8d22","#ef3e36","#df4094","#4daacc","#167ec2","#f1592a","#818285")
		// an infinitely repeating stream of colours
		val colourStream = Stream.continually(colours.toStream).flatten
		val contexts = uncoloured.map(_.parentShortName).distinct
		val contextsWithColours = contexts.zip(colourStream)
		uncoloured.map { event =>
			if (event.title == "Busy") { // FIXME hack
				event.copy(backgroundColor = "#bbb", borderColor = "#bbb")
			} else {
				val colour = contextsWithColours.find(_._1 == event.parentShortName).get._2
				event.copy(backgroundColor = colour, borderColor = colour)
			}
		}
	}
}

@Controller
@RequestMapping(value = Array("/timetable"))
class ViewMyTimetableController extends ProfilesController {
	@RequestMapping def redirect(user: CurrentUser) =
		user.profile match {
			case Some(profile) => Redirect(Routes.profile.viewTimetable(profile))
			case _ => Redirect(Routes.home)
		}
}

@Controller
@RequestMapping(value = Array("/timetable/{member}"))
class TimetableForMemberController extends AbstractTimetableController
	with AutowiringUserLookupComponent
	with AutowiringProfileServiceComponent {

	@RequestMapping
	def viewTimetable(
		@PathVariable member: Member,
		@RequestParam(value = "now", required = false) now: JLong
	) = {
		val renderDate = if (now == null) DateTime.now else new DateTime(now)
		val isSelf = member.universityId == user.universityId

		Mav("profile/view_timetable",
			"profile" -> member,
			"isSelf" -> isSelf,
			"renderDate" -> renderDate.toDate
		).crumbs(Breadcrumbs.Profile(member, isSelf))
	}

}

abstract class AbstractTimetableICalController
	extends AbstractTimetableController
		with AutowiringProfileServiceComponent
		with AutowiringTermBasedEventOccurrenceServiceComponent
		with AutowiringTermServiceComponent {

	@RequestMapping
	def getIcalFeed(@ModelAttribute("command") command: TimetableCommand): Mav = {
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

		Mav(new IcalView(cal), "filename" -> s"${command.member.universityId}.ics")
	}

}

@Controller
@RequestMapping(value = Array("/timetable/ical/{timetableHash}.ics"))
class TimetableICalController extends AbstractTimetableICalController {

	@ModelAttribute("command")
	def command(@PathVariable(value="timetableHash") timetableHash: String) = commandForTimetableHash(timetableHash)

}

@Controller
@RequestMapping(value = Array("/timetable/ical"))
class LegacyTimetableICalController extends AbstractTimetableICalController {

	@ModelAttribute("command")
	def command(@RequestParam(value="timetableHash") timetableHash: String) = commandForTimetableHash(timetableHash)

}

@Controller
@RequestMapping(value = Array("/timetable/{member}/ical", "/timetable/{member}/timetable.ics"))
class TimetableICalForMemberController extends AbstractTimetableICalController {

	@ModelAttribute("command")
	def command(@PathVariable member: Member) = commandForMember(member)

}
