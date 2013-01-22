package uk.ac.warwick.tabula.coursework.web.controllers.admin.markschemes

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._

import uk.ac.warwick.tabula.actions.Manage
import uk.ac.warwick.tabula.coursework.commands.markschemes.DeleteMarkSchemeCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markschemes/delete/{markscheme}"))
class DeleteMarkSchemeController extends CourseworkController {

	// tell @Valid annotation how to validate
	validatesSelf[DeleteMarkSchemeCommand]
	
	@ModelAttribute("command") 
	def cmd(@PathVariable department: Department, @PathVariable("markscheme") markScheme: MarkScheme) = 
		new DeleteMarkSchemeCommand(department, markScheme)
	
	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: DeleteMarkSchemeCommand): Mav = {
		Mav("admin/markschemes/delete").noLayoutIf(ajax)
	}
	
	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: DeleteMarkSchemeCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.markscheme.list(cmd.department))
		}
	}
	
}