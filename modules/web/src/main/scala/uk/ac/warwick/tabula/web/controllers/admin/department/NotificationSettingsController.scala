package uk.ac.warwick.tabula.web.controllers.admin.department

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.admin.department.NotificationSettingsCommand
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department

@Controller
@RequestMapping(Array("/admin/department/{department}/settings/notification"))
class NotificationSettingsController extends AdminController {
	
	type NotificationSettingsCommand = Appliable[Department]

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department): NotificationSettingsCommand = NotificationSettingsCommand(mandatory(department))

	@RequestMapping(method = Array(GET, HEAD))
	def form(@PathVariable("department") department: Department, @ModelAttribute("command") cmd: NotificationSettingsCommand) = {
		crumbed(Mav("admin/notification-settings",
			"returnTo" -> getReturnTo("")
		), department)
	}

	@RequestMapping(method = Array(POST))
	def saveSettings(
		@Valid @ModelAttribute("command") cmd: NotificationSettingsCommand,
		errors: Errors,
		@PathVariable("department") department: Department
	) = {
		if (errors.hasErrors) {
			form(department, cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.department(department))
		}
	}
}