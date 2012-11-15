package uk.ac.warwick.courses.web.controllers.admin.markschemes

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._

import uk.ac.warwick.courses.actions.Manage
import uk.ac.warwick.courses.commands.markschemes.DeleteMarkSchemeCommand
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.web.Routes

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markschemes/delete/{markscheme}"))
class DeleteMarkSchemeController extends BaseController {

	// tell @Valid annotation how to validate
	validatesSelf[DeleteMarkSchemeCommand]
	
	@ModelAttribute("command") 
	def cmd(@PathVariable("markscheme") markScheme: MarkScheme) = new DeleteMarkSchemeCommand(markScheme)
	
	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: DeleteMarkSchemeCommand): Mav = {
		doPermissions(cmd)
		Mav("admin/markschemes/delete").noLayoutIf(ajax)
	}
	
	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: DeleteMarkSchemeCommand, errors: Errors): Mav = {
		doPermissions(cmd)
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.markscheme.list(cmd.department))
		}
	}
	
	def doPermissions(cmd: DeleteMarkSchemeCommand) {
		mustBeAbleTo(Manage(cmd.department))
		mustBeLinked(cmd.markScheme, cmd.department)
	}
	
}