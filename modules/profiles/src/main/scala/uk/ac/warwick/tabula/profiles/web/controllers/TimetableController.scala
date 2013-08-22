package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.profiles.commands.ViewStudentPersonalTimetableCommand
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{RequestParam, RequestMapping}
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.profiles.web.views.FullCalendarEvent

@Controller
@RequestMapping(value = Array("/timetable"))
class TimetableController extends ProfilesController{


	@RequestMapping(value=Array("/show"))
	def show(){
		Mav("timetables/student")
	}
	@RequestMapping(value=Array("/api"))
	def getEvents(@RequestParam from:LocalDate, @RequestParam to:LocalDate):Mav={
		currentMember match {
			case student:StudentMember=>{
				val command = ViewStudentPersonalTimetableCommand()
				command.student = student
				command.start = from
				command.end = to
				val timetableEvents = command.apply
				val calendarEvents = timetableEvents map(FullCalendarEvent(_))
				Mav(new JSONView(calendarEvents))
			}case _ => throw new RuntimeException("Don't know how to render timetables for non-student users")
		}

	}
}
