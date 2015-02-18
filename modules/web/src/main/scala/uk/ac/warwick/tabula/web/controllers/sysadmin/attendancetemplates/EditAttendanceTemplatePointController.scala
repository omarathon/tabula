package uk.ac.warwick.tabula.web.controllers.sysadmin.attendancetemplates

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringTemplate, AttendanceMonitoringTemplatePoint}
import uk.ac.warwick.tabula.commands.sysadmin.attendancetemplates.EditAttendanceTemplatePointCommand
import uk.ac.warwick.tabula.web.controllers.sysadmin.BaseSysadminController
import uk.ac.warwick.tabula.sysadmin.web.Routes

@Controller
@RequestMapping(value = Array("/sysadmin/attendancetemplates/{template}/points/{point}/edit"))
class EditAttendanceTemplatePointController extends BaseSysadminController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable point: AttendanceMonitoringTemplatePoint) = EditAttendanceTemplatePointCommand(mandatory(point))

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringTemplatePoint] with PopulateOnForm) = {
		cmd.populate()
		render
	}

	private def render = {
		Mav("sysadmin/attendancetemplates/editpoint")
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringTemplatePoint],
		errors: Errors,
		@PathVariable template: AttendanceMonitoringTemplate
	) = {
		if (errors.hasErrors) {
			render
		} else {
			cmd.apply()
			Redirect(Routes.AttendanceTemplates.edit(template))
		}
	}

}
