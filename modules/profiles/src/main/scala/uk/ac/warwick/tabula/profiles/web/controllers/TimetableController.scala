package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.profiles.commands.{ViewStudentPersonalTimetableCommandState, ViewStudentPersonalTimetableCommand}
import uk.ac.warwick.tabula.data.model.{Member, StudentMember}
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{ModelAttribute, RequestParam, RequestMapping}
import org.joda.time.{DateTime}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.profiles.web.views.FullCalendarEvent
import uk.ac.warwick.tabula.profiles.services.timetables._
import uk.ac.warwick.tabula.services.{UserLookupService, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable

@Controller
@RequestMapping(value = Array("/timetable"))
class TimetableController extends ProfilesController {

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
	def command( @RequestParam whoFor:Member) = {
		whoFor match {
			case student: StudentMember => {
				ViewStudentPersonalTimetableCommand(eventSource, student)
			}
			case _ => throw new RuntimeException("Don't know how to render timetables for non-student users")
		}

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
		val timetableEvents = command.apply
		val calendarEvents = timetableEvents map (FullCalendarEvent(_, userLookup))
		Mav(new JSONView(colourEvents(calendarEvents)))
	}

	def colourEvents(uncoloured:Seq[FullCalendarEvent]):Seq[FullCalendarEvent] = {
		val colours = Seq("#239b92","#a3b139","#ec8d22","#ef3e36","#df4094","#4daacc","#167ec2","#f1592a","#818285")
		// an infinitely repeating stream of colours
 		val colourStream = Stream.continually(colours.toStream).flatten
		val modules = uncoloured.map(_.moduleCode).distinct
		val modulesWithColours = modules.zip(colourStream)
		uncoloured map (event=>event.copy(backgroundColor = modulesWithColours.find(_._1 == event.moduleCode).get._2))
	}
}

