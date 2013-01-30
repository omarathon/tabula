package uk.ac.warwick.tabula.coursework.web.controllers.admin.markschemes

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._

import uk.ac.warwick.tabula.coursework.commands.markschemes.AddMarkSchemeCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markschemes/add"))
class AddMarkSchemeController extends CourseworkController {

	// tell @Valid annotation how to validate
	validatesSelf[AddMarkSchemeCommand]
	
	@ModelAttribute("command") 
	def cmd(@PathVariable("department") department: Department) = new AddMarkSchemeCommand(department)
	
	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: AddMarkSchemeCommand): Mav = {
		doBind(cmd)
		Mav("admin/markschemes/add")
	}
	
	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: AddMarkSchemeCommand, errors: Errors): Mav = {
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