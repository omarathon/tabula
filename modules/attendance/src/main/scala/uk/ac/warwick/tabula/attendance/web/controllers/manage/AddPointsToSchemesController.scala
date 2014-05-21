package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.commands.manage.AddPointsToSchemesCommand
import uk.ac.warwick.tabula.AcademicYear

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/addpoints"))
class AddPointsToSchemesController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		AddPointsToSchemesCommand(department, academicYear)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Map[AttendanceMonitoringScheme, Boolean]]) = {
		Mav("manage/addpoints", "schemeMap" -> cmd.apply())
	}

}
