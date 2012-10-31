package uk.ac.warwick.courses.web.controllers.admin.markschemes

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._

import uk.ac.warwick.courses.actions.Manage
import uk.ac.warwick.courses.commands.markschemes.AddMarkSchemeCommand
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.web.Routes

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markschemes/add"))
class AddMarkSchemeController extends BaseController {

	// tell @Valid annotation how to validate
	validatesSelf[AddMarkSchemeCommand]
	
	@ModelAttribute("command") 
	def cmd(@PathVariable department: Department) = new AddMarkSchemeCommand(department)
	
	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: AddMarkSchemeCommand): Mav = {
		mustBeAbleTo(Manage(cmd.department))
		doBind(cmd)
		Mav("admin/markschemes/add")
	}
	
	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: AddMarkSchemeCommand, errors: Errors): Mav = {
		mustBeAbleTo(Manage(cmd.department))
		doBind(cmd)
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.markscheme.list(cmd.department))
		}
	}
	
	// do extra property processing on the form.
	def doBind(cmd: AddMarkSchemeCommand) = cmd.doBind()
	
}