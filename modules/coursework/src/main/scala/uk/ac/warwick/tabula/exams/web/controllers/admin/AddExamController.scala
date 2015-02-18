package uk.ac.warwick.tabula.exams.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.exams.commands.{AddExamCommandState, AddExamCommand}
import uk.ac.warwick.tabula.web.controllers.BaseController
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/exam/new"))
class AddExamController extends BaseController {

	type AddExamCommand = Appliable[Exam] with AddExamCommandState

	@ModelAttribute("command")
	def command(@PathVariable("module") module: Module) = AddExamCommand(mandatory(module))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@ModelAttribute("command") cmd: AddExamCommand) = Mav("exams/admin/module/new")

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: AddExamCommand, errors: Errors) = {
		if (errors.hasErrors) {
			showForm(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.home)
		}
	}

}
