package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.profiles.commands.{ViewStudentPersonalTimetableCommandState, ViewStudentPersonalTimetableCommand}
import uk.ac.warwick.tabula.data.model.{Member, StudentMember}
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{ModelAttribute, RequestParam, RequestMapping}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.profiles.web.views.FullCalendarEvent
import uk.ac.warwick.tabula.profiles.services.timetables._
import uk.ac.warwick.tabula.services.{AutowiringSecurityServiceComponent, AutowiringMeetingRecordServiceComponent, AutowiringRelationshipServiceComponent, UserLookupService, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.timetables.EventOccurrence
import uk.ac.warwick.tabula.CurrentUser

@Controller
@RequestMapping(value = Array("/timetable"))
class TimetableController extends ProfilesController {

	var userLookup = Wire[UserLookupService]

	// re-use the event source, so it can cache lookups between requests
	val timetableEventSource = (new CombinedStudentTimetableEventSourceComponent
		with SmallGroupEventTimetableEventSourceComponentImpl
		with ScientiaHttpTimetableFetchingServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringUserLookupComponent
		with AutowiringScientiaConfigurationComponent
		with SystemClockComponent
		).studentTimetableEventSource

	val scheduledMeetingEventSource = (new MeetingRecordServiceScheduledMeetingEventSourceComponent
		with AutowiringRelationshipServiceComponent
		with AutowiringMeetingRecordServiceComponent
		with AutowiringSecurityServiceComponent
	).scheduledMeetingEventSource

	@ModelAttribute("command")
	def command(@RequestParam whoFor: Member, user: CurrentUser) = {
		whoFor match {
			case student: StudentMember => {
				ViewStudentPersonalTimetableCommand(timetableEventSource, scheduledMeetingEventSource, student, user)
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

