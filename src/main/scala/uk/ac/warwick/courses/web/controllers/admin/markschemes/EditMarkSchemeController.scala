package uk.ac.warwick.courses.web.controllers.admin.markschemes

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._

import uk.ac.warwick.courses.actions.Manage
import uk.ac.warwick.courses.commands.markschemes.EditMarkSchemeCommand
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.web.Routes

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markschemes/edit/{markscheme}"))
class EditMarkSchemeController extends BaseController {

	// tell @Valid annotation how to validate
	validatesSelf[EditMarkSchemeCommand]
	
	@ModelAttribute("command") 
	def cmd(@PathVariable department: Department, @PathVariable markscheme: MarkScheme) = 
		new EditMarkSchemeCommand(department, markscheme)
	 
	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: EditMarkSchemeCommand): Mav = {
		mustBeAbleTo(Manage(cmd.department))
		doBind(cmd)
		Mav("admin/markschemes/edit")
	}
	
	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: EditMarkSchemeCommand, errors: Errors): Mav = {
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
	def doBind(cmd: EditMarkSchemeCommand) = cmd.doBind()
	
}