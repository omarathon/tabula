package uk.ac.warwick.tabula.home.web.controllers.sysadmin.attendancetemplates

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringTemplate, AttendanceMonitoringTemplatePoint}
import uk.ac.warwick.tabula.home.commands.sysadmin.attendancetemplates.DeleteAttendanceTemplatePointCommand
import uk.ac.warwick.tabula.home.web.controllers.sysadmin.BaseSysadminController
import uk.ac.warwick.tabula.sysadmin.web.Routes

@Controller
@RequestMapping(value = Array("/sysadmin/attendancetemplates/{template}/points/{point}/delete"))
class DeleteAttendanceTemplatePointController extends BaseSysadminController {

	@ModelAttribute("command")
	def command(@PathVariable point: AttendanceMonitoringTemplatePoint) = DeleteAttendanceTemplatePointCommand(mandatory(point))

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[Unit]) = {
		render
	}

	private def render = {
		Mav("sysadmin/attendancetemplates/deletepoint")
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@ModelAttribute("command") cmd: Appliable[Unit],
		@PathVariable template: AttendanceMonitoringTemplate
	) = {
		cmd.apply()
		Redirect(Routes.AttendanceTemplates.edit(template))
	}

}
