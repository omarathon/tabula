package uk.ac.warwick.tabula.home.web.controllers.sysadmin.attendancetemplates

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.home.commands.sysadmin.attendancetemplates.ListAttendanceTemplatesCommand
import uk.ac.warwick.tabula.home.web.controllers.sysadmin.BaseSysadminController

@Controller
@RequestMapping(value = Array("/sysadmin/attendancetemplates"))
class ListAttendanceTemplatesController extends BaseSysadminController {

	@ModelAttribute("command")
	def command = ListAttendanceTemplatesCommand()

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringTemplate]]) = {
		Mav("sysadmin/attendancetemplates/home", "templates" -> cmd.apply())
	}

}
