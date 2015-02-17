package uk.ac.warwick.tabula.web.web.controllers.sysadmin.attendancetemplates

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates.CreateAttendanceTemplateCommand
import uk.ac.warwick.tabula.web.web.controllers.sysadmin.BaseSysadminController
import uk.ac.warwick.tabula.sysadmin.web.Routes

@Controller
@RequestMapping(value = Array("/sysadmin/attendancetemplates/add"))
class CreateAttendanceTemplateController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command = CreateAttendanceTemplateCommand()

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringTemplate]) = {
		render
	}

	private def render = {
		Mav("sysadmin/attendancetemplates/new")
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringTemplate],
		errors: Errors
	) = {
		if (errors.hasErrors) {
			render
		} else {
			cmd.apply()
			Redirect(Routes.AttendanceTemplates.list)
		}
	}

}
