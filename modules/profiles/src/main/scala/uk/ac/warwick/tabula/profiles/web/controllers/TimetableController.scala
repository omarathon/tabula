package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.profiles.commands.ViewStudentPersonalTimetableCommand
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{RequestParam, RequestMapping}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.profiles.web.views.FullCalendarEvent
import uk.ac.warwick.tabula.profiles.services.timetables.{AutowiringScientiaConfigurationComponent, ScientiaHttpTimetableFetchingServiceComponent, SmallGroupEventTimetableEventSourceComponentImpl, CombinedStudentTimetableEventSourceComponent}
import uk.ac.warwick.tabula.services.{UserLookupService, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.spring.Wire

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


	@RequestMapping(value = Array("/api"))
	def getEvents(@RequestParam from: Long, @RequestParam to: Long): Mav = {
		// from and to are seconds since the epoch, because that's what FullCalendar likes to send. Sigh.
		val start = new DateTime(from * 1000).toLocalDate
		val end = new DateTime(to * 1000).toLocalDate
		currentMember match {
			case student: StudentMember => {
				val command = ViewStudentPersonalTimetableCommand(eventSource)
				command.student = student
				command.start = start
				command.end = end
				val timetableEvents = command.apply
				val calendarEvents = timetableEvents map (FullCalendarEvent(_, userLookup))
				Mav(new JSONView(colourEvents(calendarEvents)))
			}
			case _ => throw new RuntimeException("Don't know how to render timetables for non-student users")
		}

	}

	def colourEvents(uncoloured:Seq[FullCalendarEvent]):Seq[FullCalendarEvent] = {
		val colours = Seq("#239b92","#93225f","#a3b139","#3c5f80","#ec8d22","#ef3e36","#b0302e","#524761")++
			// the second line are the colours listed in "variables.less" as "related colour"
			Seq("#185c54","#df4094","#4daacc","#167ec2","#f1592a","#818285","#3a3a3c")
		// an infinitely repeating stream of colours
 		val colourStream = Stream.continually(colours.toStream).flatten
		val modules = uncoloured.map(_.moduleCode).distinct
		val modulesWithColours = modules.zip(colourStream)
		uncoloured map (event=>event.copy(backgroundColor = modulesWithColours.find(_._1 == event.moduleCode).get._2))
	}
}

