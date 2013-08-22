package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.profiles.commands.ViewStudentPersonalTimetableCommand
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{RequestParam, RequestMapping}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.profiles.web.views.FullCalendarEvent

@Controller
@RequestMapping(value = Array("/timetable"))
class TimetableController extends ProfilesController{


	@RequestMapping(value=Array("/show"))
	def show() = {
		Mav("timetables/student")
	}
	@RequestMapping(value=Array("/api"))
	def getEvents(@RequestParam from:Long, @RequestParam to:Long):Mav={
		// from and to are seconds since the epoch, because that's what FullCalendar likes to send. Sigh.
		val start = new DateTime(from*1000).toLocalDate
		val end = new DateTime(to*1000).toLocalDate
		currentMember match {
			case student:StudentMember=>{
				val command = ViewStudentPersonalTimetableCommand()
				command.student = student
				command.start = start
				command.end = end
				val timetableEvents = command.apply
				val calendarEvents = timetableEvents map(FullCalendarEvent(_))
				Mav(new JSONView(calendarEvents))
			}case _ => throw new RuntimeException("Don't know how to render timetables for non-student users")
		}

	}
}
