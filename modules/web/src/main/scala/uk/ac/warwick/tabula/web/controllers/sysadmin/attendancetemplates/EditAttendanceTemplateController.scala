package uk.ac.warwick.tabula.web.controllers.sysadmin.attendancetemplates

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates.EditAttendanceTemplateCommand
import uk.ac.warwick.tabula.web.controllers.sysadmin.BaseSysadminController
import uk.ac.warwick.tabula.sysadmin.web.Routes

@Controller
@RequestMapping(value = Array("/sysadmin/attendancetemplates/{template}/edit"))
class EditAttendanceTemplateController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable template: AttendanceMonitoringTemplate) = EditAttendanceTemplateCommand(mandatory(template))

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringTemplate] with PopulateOnForm) = {
		cmd.populate()
		render
	}

	private def render = {
		Mav("sysadmin/attendancetemplates/edit")
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
