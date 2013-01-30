package uk.ac.warwick.tabula.coursework.web.controllers.admin.markschemes

import javax.validation.Valid
import scala.collection.JavaConversions._
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.coursework.commands.markschemes.EditMarkSchemeCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.MarkSchemeDao
import uk.ac.warwick.spring.Wire

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markschemes/edit/{markscheme}"))
class EditMarkSchemeController extends CourseworkController {

	// tell @Valid annotation how to validate
	validatesSelf[EditMarkSchemeCommand]
	
	@ModelAttribute("command") 
	def cmd(@PathVariable("department") department: Department, @PathVariable("markscheme") markscheme: MarkScheme) = 
		new EditMarkSchemeCommand(department, markscheme)
	 
	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: EditMarkSchemeCommand): Mav = {
		doBind(cmd)
		Mav("admin/markschemes/edit")
	}
	
	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: EditMarkSchemeCommand, errors: Errors): Mav = {
		doBind(cmd)
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.markscheme.list(cmd.department))
		}
	}
	
	// do extra property processing on the form.
	def doBind(cmd: EditMarkSchemeCommand) {
		cmd.doBind()
	}
	
}