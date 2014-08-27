package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.profiles.commands.{PublicStaffPersonalTimetableCommand, PersonalTimetableCommandState, ViewStaffPersonalTimetableCommand, PublicStudentPersonalTimetableCommand, ViewStudentPersonalTimetableCommandState, ViewStudentPersonalTimetableCommand}
import uk.ac.warwick.tabula.data.model.{StaffMember, Member, StudentMember}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.IcalView
import org.springframework.web.bind.annotation.{ModelAttribute, RequestParam, RequestMapping}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.profiles.web.views.FullCalendarEvent
import uk.ac.warwick.tabula.profiles.services.timetables._
import uk.ac.warwick.tabula.services.{TermAwareWeekToDateConverterComponent, AutowiringProfileServiceComponent, AutowiringTermServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringMeetingRecordServiceComponent, AutowiringRelationshipServiceComponent, UserLookupService, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable
import net.fortuna.ical4j.model.{TimeZoneRegistryFactory, Calendar}
import net.fortuna.ical4j.model.property.{XProperty, Method, CalScale, Version, ProdId}
import net.fortuna.ical4j.model.component.VEvent
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.timetables.EventOccurrence

@Controller
@RequestMapping(value = Array("/timetable"))
class TimetableController extends ProfilesController with TermBasedEventOccurrenceComponent with AutowiringTermServiceComponent
	with AutowiringProfileServiceComponent with TermAwareWeekToDateConverterComponent {

	var userLookup = Wire[UserLookupService]

	// re-use the event source, so it can cache lookups between requests
	val studentTimetableEventSource = (new CombinedStudentTimetableEventSourceComponent
		with SmallGroupEventTimetableEventSourceComponentImpl
		with CombinedHttpTimetableFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringScientiaConfigurationComponent
		with AutowiringCelcatConfigurationComponent
		with SystemClockComponent
		).studentTimetableEventSource

	val staffTimetableEventSource = (new CombinedStaffTimetableEventSourceComponent
		with SmallGroupEventTimetableEventSourceComponentImpl
		with CombinedHttpTimetableFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringScientiaConfigurationComponent
		with AutowiringCelcatConfigurationComponent
		with SystemClockComponent
		).staffTimetableEventSource

	val scheduledMeetingEventSource = (new MeetingRecordServiceScheduledMeetingEventSourceComponent
		with AutowiringRelationshipServiceComponent
		with AutowiringMeetingRecordServiceComponent
		with AutowiringSecurityServiceComponent
	).scheduledMeetingEventSource

	@ModelAttribute("command")
	def command(
	  @RequestParam(value="whoFor", required=false) whoFor:Member,
	  @RequestParam(value="timetableHash", required=false) timetableHash:String
   	): Appliable[Seq[EventOccurrence]] with PersonalTimetableCommandState = {
	  if (timetableHash.hasText) {
		  profileService.getMemberByTimetableHash(timetableHash).map {
				case student: StudentMember => PublicStudentPersonalTimetableCommand(studentTimetableEventSource, scheduledMeetingEventSource, student, user)
				case staff: StaffMember => PublicStaffPersonalTimetableCommand(staffTimetableEventSource, scheduledMeetingEventSource, staff, user)
			}.getOrElse(throw new ItemNotFoundException)
	  } else {
		  whoFor match {
				case student: StudentMember => ViewStudentPersonalTimetableCommand(studentTimetableEventSource, scheduledMeetingEventSource, student, user)
				case staff: StaffMember => ViewStaffPersonalTimetableCommand(staffTimetableEventSource, scheduledMeetingEventSource, staff, user)
				case _ => throw new RuntimeException("Don't know how to render timetables for non-student or non-staff users")
			}
	  }
	}

	@RequestMapping(value = Array("/ical"))
	def getIcalFeed(@ModelAttribute("command") command: Appliable[Seq[EventOccurrence]] with PersonalTimetableCommandState): Mav = {
		val year = AcademicYear.findAcademicYearContainingDate(DateTime.now, termService)

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

		command.start = start
		command.end = end

		val timetableEvents = command.apply()

		val cal: Calendar = new Calendar
		cal.getProperties.add(new ProdId("-//Tabula//University of Warwick IT Services//EN"))
		cal.getProperties.add(Version.VERSION_2_0)
		cal.getProperties.add(CalScale.GREGORIAN)
		cal.getProperties.add(Method.PUBLISH)
		cal.getProperties.add(new XProperty("X-PUBLISHED-TTL", "PT12H"))
		cal.getProperties.add(new XProperty("X-WR-CALNAME", s"Tabula timetable - ${command.member.universityId}"))
		cal.getProperties.add(new XProperty("X-WR-TIMEZONE", "Europe/London"))
		cal.getProperties.add(new XProperty("X-LIC-LOCATION", "Europe/London"))
		val vTimezone = TimeZoneRegistryFactory.getInstance.createRegistry.getTimeZone("Europe/London").getVTimeZone
		cal.getComponents.add(vTimezone)

		for (event <- timetableEvents) {
			val vEvent: VEvent = eventOccurrenceService.toVEvent(event)
			vEvent.getProperties.add(vTimezone.getTimeZoneId)
			cal.getComponents.add(vEvent)
		}

		Mav(new IcalView(cal), "filename" -> s"${command.member.universityId}.ics")
	}

	@RequestMapping(value = Array("/api"))
	def getEvents(
		@RequestParam from: Long,
		@RequestParam to: Long,
		@ModelAttribute("command") command:Appliable[Seq[EventOccurrence]] with PersonalTimetableCommandState
	): Mav = {
		// from and to are seconds since the epoch, because that's what FullCalendar likes to send.
		// This conversion could move onto the command, if anyone felt strongly that it was a concern of the command
		// or we could write an EpochSecondsToDateTime 2-way converter.
		val start = new DateTime(from * 1000).toLocalDate
		val end = new DateTime(to * 1000).toLocalDate
		command.start = start
		command.end = end
		val timetableEvents = command.apply()
		val calendarEvents = timetableEvents.map (FullCalendarEvent(_, userLookup))
		Mav(new JSONView(colourEvents(calendarEvents)))
	}

	def colourEvents(uncoloured:Seq[FullCalendarEvent]):Seq[FullCalendarEvent] = {
		val colours = Seq("#239b92","#a3b139","#ec8d22","#ef3e36","#df4094","#4daacc","#167ec2","#f1592a","#818285")
		// an infinitely repeating stream of colours
 		val colourStream = Stream.continually(colours.toStream).flatten
		val contexts = uncoloured.map(_.context).distinct
		val contextsWithColours = contexts.zip(colourStream)
		uncoloured.map { event =>
			if (event.title == "Busy") { // FIXME hack
				event.copy(backgroundColor = "#bbb", borderColor = "#bbb")
			} else {
				val colour = contextsWithColours.find(_._1 == event.context).get._2
				event.copy(backgroundColor = colour, borderColor = colour)
			}
		}
	}
}

