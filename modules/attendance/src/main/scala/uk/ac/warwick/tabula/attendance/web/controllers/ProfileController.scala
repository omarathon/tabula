package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.commands.ProfileCommand
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/profile/{studentCourseDetails}/{academicYear}"))
class ProfileController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable studentCourseDetails: StudentCourseDetails, @PathVariable academicYear: AcademicYear)
		= ProfileCommand(studentCourseDetails, academicYear)

	@RequestMapping
	def render(@ModelAttribute("command") cmd: Appliable[Unit], currentUser: User) = {
		cmd.apply()
		Mav("home/profile", "currentUser" -> currentUser).noLayoutIf(ajax)
	}
}
