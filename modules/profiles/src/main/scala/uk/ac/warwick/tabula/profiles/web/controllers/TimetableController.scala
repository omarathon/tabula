package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.profiles.commands.ViewStudentPersonalTimetableCommand
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.{RequestParam, RequestMapping}
import org.joda.time.LocalDate

@Controller
@RequestMapping(value = Array("/timetable"))
class TimetableController extends ProfilesController{


	@RequestMapping
	def get(@RequestParam from:LocalDate, @RequestParam to:LocalDate):Mav={
		currentMember match {
			case student:StudentMember=>{
				val command = ViewStudentPersonalTimetableCommand()
				command.student = student
				command.start = from
				command.end = to
				val timetable = command.apply
				Mav("timetables/student","timetable"->timetable)
			}case _ => throw new RuntimeException("Don't know how to render timetables for non-student users")
		}

	}
}
