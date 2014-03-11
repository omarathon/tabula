package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.profiles.commands.{PublicStudentPersonalTimetableCommand, ViewStudentPersonalTimetableCommandState, ViewStudentPersonalTimetableCommand}
import uk.ac.warwick.tabula.data.model.{Member, StudentMember}
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, RequestMapping}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.web.views.{IcalView, JSONView}
import uk.ac.warwick.tabula.profiles.web.views.FullCalendarEvent
import uk.ac.warwick.tabula.profiles.services.timetables._
import uk.ac.warwick.tabula.services.{TermAwareWeekToDateConverterComponent, AutowiringProfileServiceComponent, AutowiringTermServiceComponent, UserLookupService, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable
import net.fortuna.ical4j.model.{TimeZoneRegistryFactory, Calendar}
import net.fortuna.ical4j.model.property.{XProperty, Method, CalScale, Version, ProdId}
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.data.CalendarOutputter
import java.io.StringWriter
import uk.ac.warwick.tabula.PermissionDeniedException

@Controller
@RequestMapping(value = Array("/timetable"))
class TimetableController extends ProfilesController with TermBasedEventOccurrenceComponent with AutowiringTermServiceComponent
with AutowiringProfileServiceComponent with TermAwareWeekToDateConverterComponent {

	var userLookup = Wire[UserLookupService]

	// re-use the event source, so it can cache lookups between requests
	val eventSource = (new CombinedStudentTimetableEventSourceComponent
		with SmallGroupEventTimetableEventSourceComponentImpl
		with ScientiaHttpTimetableFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringScientiaConfigurationComponent
		with SystemClockComponent
		).studentTimetableEventSource

	@ModelAttribute("command")
	def command(
			@RequestParam(value="whoFor", required=false) whoFor:Member,
			@RequestParam(value="timetableHash", required=false) timetableHash:String) = {
		if (timetableHash.hasText) {
			profileService.getStudentMemberByTimetableHash(timetableHash).map {
				member => PublicStudentPersonalTimetableCommand(eventSource, member)
			}.getOrElse(throw new IllegalArgumentException)
		}
		else {
			whoFor match {
				case student: StudentMember => {
					ViewStudentPersonalTimetableCommand(eventSource, student)
				}
				case _ => throw new RuntimeException("Don't know how to render timetables for non-student users")
			}
		}

	}

	@RequestMapping(value = Array("/ical"))
	def getIcalFeed(@ModelAttribute("command") command:Appliable[Seq[EventOccurrence]] with ViewStudentPersonalTimetableCommandState): Mav = {

		val timetableEvents = command.apply()

		val cal: Calendar = new Calendar
		cal.getProperties.add(new ProdId("-//Tabula//University of Warwick IT Services//EN"))
		cal.getProperties.add(Version.VERSION_2_0)
		cal.getProperties.add(CalScale.GREGORIAN)
		cal.getProperties.add(Method.PUBLISH)
		cal.getProperties.add(new XProperty("X-WR-TIMEZONE", "Europe/London"))
		cal.getProperties.add(new XProperty("X-LIC-LOCATION", "Europe/London"))
		val vTimezone = TimeZoneRegistryFactory.getInstance.createRegistry.getTimeZone("Europe/London").getVTimeZone
		cal.getComponents.add(vTimezone)

		for (event <- timetableEvents) {
			val vEvent: VEvent = eventOccurrenceService.toVEvent(event)
			vEvent.getProperties.add(vTimezone.getTimeZoneId)
			cal.getComponents.add(vEvent)
		}

		Mav(new IcalView(cal))
	}

	@RequestMapping(value = Array("/api"))
	def getEvents(@RequestParam from: Long, @RequestParam to: Long, @ModelAttribute("command") command:Appliable[Seq[EventOccurrence]] with ViewStudentPersonalTimetableCommandState): Mav = {
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
		val modules = uncoloured.map(_.moduleCode).distinct
		val modulesWithColours = modules.zip(colourStream)
		uncoloured map (event=>{
			val colour = modulesWithColours.find(_._1 == event.moduleCode).get._2
			event.copy(backgroundColor = colour, borderColor = colour)})
	}
}

