package uk.ac.warwick.tabula.admin.web.controllers.department

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.admin.commands.department.{NotificationSettingsCommand, DisplaySettingsCommand}
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.admin.web.controllers.AdminController
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.RelationshipService

@Controller
@RequestMapping(Array("/department/{department}/settings/notification"))
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
			Redirect(Routes.department(department))
		}
	}
}